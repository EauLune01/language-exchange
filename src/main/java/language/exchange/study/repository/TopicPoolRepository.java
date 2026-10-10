package language.exchange.study.repository;

import language.exchange.global.constants.redis.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * 방마다 "아직 안 쓴 주제 id"를 Redis Set 으로 들고 있는 풀입니다. 기록의 기준은 DB 이고, 풀은 DB 에서 언제든 다시 만들 수 있습니다.
 *  - 뽑기는 SPOP 이라 두 요청이 같은 주제를 받을 수 없습니다. (DB 의 사용 날짜는 큐로 나중에 기록돼서, DB 만 보고 고르면 겹칠 수 있다)
 *  - 빈 Set 은 Redis 가 키째로 지워서 "다 썼음"과 "아직 안 읽어 옴"을 구분할 수 없으므로, 읽어 왔다는 표시 키를 따로 둡니다.
 *  - 뽑힌 주제는 사용 날짜가 DB 에 기록될 때까지 DB 에서는 안 쓴 주제로 보입니다. 그 사이에 풀을 다시 읽어 와도 되살아나지 않게,
 *    방금 뽑힌 주제 id 를 잠깐 따로 들고 있습니다.
 *  - 여러 명령이 한 덩어리로 실행돼야 하는 곳은 Lua 스크립트로 묶었습니다. KEYS = [풀, 표시, 방금 뽑힌 주제], ARGV[1] = TTL(초)
 */
@Repository
@RequiredArgsConstructor
public class TopicPoolRepository {

    public static final long NOT_LOADED = -1;
    public static final long EMPTY = 0;

    // 7일 동안 아무도 안 뽑은 방의 풀은 사라지고, 다음에 뽑을 때 DB 에서 다시 읽는다 (뽑을 때마다 연장)
    private static final String TTL_SECONDS = String.valueOf(Duration.ofDays(7).toSeconds());

    // 방금 뽑힌 주제를 기억하는 시간. 사용 날짜가 DB 에 기록될 때까지(Consumer 재시도 포함 몇 초)보다 넉넉하면 된다
    private static final String DRAWN_TTL_SECONDS = String.valueOf(Duration.ofMinutes(10).toSeconds());

    // ARGV[2] = 패스한 주제 id (뽑기면 빈 문자열), ARGV[3] = 방금 뽑힌 주제의 TTL(초)
    // 패스한 주제는 고르지 않고, 다른 주제를 골랐을 때만 풀로 돌려놓는다
    private static final RedisScript<Long> POP = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 0 then return -1 end
            local passed = ARGV[2]
            local wasInPool = passed ~= '' and redis.call('SREM', KEYS[1], passed) == 1
            local id = redis.call('SPOP', KEYS[1])
            if passed ~= '' and (id or wasInPool) then redis.call('SADD', KEYS[1], passed) end
            if id then
                if passed ~= '' then redis.call('SREM', KEYS[3], passed) end
                redis.call('SADD', KEYS[3], id)
                redis.call('EXPIRE', KEYS[3], ARGV[3])
            end
            redis.call('EXPIRE', KEYS[1], ARGV[1])
            redis.call('EXPIRE', KEYS[2], ARGV[1])
            return id and tonumber(id) or 0
            """, Long.class);

    // ARGV[2..] = 주제 id 들. 다른 요청이 먼저 읽어 왔으면 아무것도 하지 않고, 방금 뽑힌 주제는 넣지 않는다
    private static final RedisScript<Long> LOAD = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
            for i = 2, #ARGV do
                if redis.call('SISMEMBER', KEYS[3], ARGV[i]) == 0 then redis.call('SADD', KEYS[1], ARGV[i]) end
            end
            redis.call('EXPIRE', KEYS[1], ARGV[1])
            redis.call('SET', KEYS[2], '1', 'EX', ARGV[1])
            return 1
            """, Long.class);

    // ARGV[2] = 주제 id
    private static final RedisScript<Long> ADD = new DefaultRedisScript<>("""
            redis.call('SADD', KEYS[1], ARGV[2])
            return redis.call('EXPIRE', KEYS[1], ARGV[1])
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 풀에서 주제 id 하나를 무작위로 꺼냅니다. 아직 DB 에서 읽어 오지 않았으면 NOT_LOADED, 남은 주제가 없으면 EMPTY.
     * passedId 가 있으면 패스입니다: 그 주제는 고르지 않고, 다른 주제를 꺼냈다면 그 주제를 풀로 돌려놓습니다.
     */
    public long pop(Long roomId, Long passedId) {
        return redisTemplate.execute(POP, keys(roomId), TTL_SECONDS, passedId == null ? "" : passedId.toString(), DRAWN_TTL_SECONDS);
    }

    /** DB 에서 읽은 안 쓴 주제 id 들로 풀을 채우고, 읽어 왔다고 표시합니다. */
    public void load(Long roomId, List<Long> unusedTopicIds) {
        Object[] args = Stream.concat(Stream.of(TTL_SECONDS), unusedTopicIds.stream().map(String::valueOf)).toArray();
        redisTemplate.execute(LOAD, keys(roomId), args);
    }

    /** 새로 등록한 주제를 풀에 넣습니다. 아직 읽어 오기 전이어도 넣어 둡니다 (읽어 올 때 합쳐진다). */
    public void add(Long roomId, Long topicId) {
        redisTemplate.execute(ADD, keys(roomId), TTL_SECONDS, topicId.toString());
    }

    /** 다음에 뽑을 때 DB 에서 다시 읽어 오게 합니다. */
    public void invalidate(Long roomId) {
        redisTemplate.delete(keys(roomId).get(1));
    }

    private List<String> keys(Long roomId) {
        return List.of(
                RedisKeyConstants.format(RedisKeyConstants.TOPIC_UNUSED_POOL, roomId),
                RedisKeyConstants.format(RedisKeyConstants.TOPIC_UNUSED_READY, roomId),
                RedisKeyConstants.format(RedisKeyConstants.TOPIC_DRAWN, roomId));
    }
}
