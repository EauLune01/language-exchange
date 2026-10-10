package language.exchange.study.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 안 쓴 주제 풀을 실제 Redis 로 확인합니다. (Lua 스크립트는 Redis 없이는 확인할 수 없어서)
 * localhost:6379 에 Redis 가 없으면 건너뜁니다: docker compose up -d redis
 * 개발용 데이터와 섞이지 않게 15번 DB 에 겹치지 않을 방 번호로 쓰고, 끝나면 지웁니다.
 */
class TopicPoolRepositoryTest {

    private static final int TEST_DATABASE = 15;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private TopicPoolRepository topicPoolRepository;
    private Long roomId;

    @BeforeEach
    void setUp() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("localhost", 6379);
        configuration.setDatabase(TEST_DATABASE);
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        assumeTrue(redisIsUp(), "Redis is not running on localhost:6379");

        redisTemplate = new StringRedisTemplate(connectionFactory);
        topicPoolRepository = new TopicPoolRepository(redisTemplate);
        roomId = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null) {
            redisTemplate.delete(List.of("topics:" + roomId + ":unused", "topics:" + roomId + ":unused-ready",
                    "topics:" + roomId + ":drawn"));
        }
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("DB 에서 읽어 오기 전에는 NOT_LOADED, 다 꺼내면 EMPTY")
    void popTellsNotLoadedFromEmpty() {
        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(TopicPoolRepository.NOT_LOADED);

        topicPoolRepository.load(roomId, List.of(1L));

        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(1L);
        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(TopicPoolRepository.EMPTY);
    }

    @Test
    @DisplayName("동시에 뽑아도 같은 주제가 두 번 나오지 않는다")
    void concurrentPopsNeverReturnTheSameTopic() throws Exception {
        int topicCount = 200;
        topicPoolRepository.load(roomId, LongStream.rangeClosed(1, topicCount).boxed().toList());

        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            Callable<Long> draw = () -> topicPoolRepository.pop(roomId, null);
            Set<Long> drawn = new HashSet<>();
            for (Future<Long> future : executor.invokeAll(Collections.nCopies(topicCount, draw))) {
                drawn.add(future.get());
            }
            assertThat(drawn).hasSize(topicCount).doesNotContain(TopicPoolRepository.EMPTY);
        } finally {
            executor.shutdown();
        }
        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(TopicPoolRepository.EMPTY);
    }

    @Test
    @DisplayName("패스하면 다른 주제가 나오고, 패스한 주제는 풀로 돌아간다")
    void passReturnsTopicToPool() {
        topicPoolRepository.load(roomId, List.of(1L, 2L));
        long drawn = topicPoolRepository.pop(roomId, null);

        long next = topicPoolRepository.pop(roomId, drawn);

        assertThat(next).isNotEqualTo(drawn).isPositive();
        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(drawn);
    }

    @Test
    @DisplayName("대신 뽑을 주제가 없으면 패스해도 풀이 그대로다")
    void passWithoutAnotherTopicChangesNothing() {
        topicPoolRepository.load(roomId, List.of(1L));
        long drawn = topicPoolRepository.pop(roomId, null);

        assertThat(topicPoolRepository.pop(roomId, drawn)).isEqualTo(TopicPoolRepository.EMPTY);
        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(TopicPoolRepository.EMPTY);
    }

    @Test
    @DisplayName("아직 풀에 있는 주제를 패스하면 그 주제는 고르지 않는다")
    void passNeverPicksThePassedTopic() {
        topicPoolRepository.load(roomId, List.of(1L, 2L));

        assertThat(topicPoolRepository.pop(roomId, 1L)).isEqualTo(2L);
        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 읽어 온 풀은 다시 읽어 와도 뽑힌 주제가 되살아나지 않고, 읽어 오기 전에 넣은 주제는 합쳐진다")
    void loadMergesAndRunsOnce() {
        topicPoolRepository.add(roomId, 3L);
        topicPoolRepository.load(roomId, List.of(1L));
        Set<Long> drawn = Set.of(topicPoolRepository.pop(roomId, null), topicPoolRepository.pop(roomId, null));
        assertThat(drawn).containsExactlyInAnyOrder(1L, 3L);

        topicPoolRepository.load(roomId, List.of(1L, 3L));

        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(TopicPoolRepository.EMPTY);
    }

    @Test
    @DisplayName("다시 읽어 와도 방금 뽑힌 주제(DB 에는 아직 안 쓴 주제로 보임)는 되살아나지 않는다")
    void reloadDoesNotBringBackJustDrawnTopic() {
        topicPoolRepository.load(roomId, List.of(1L, 2L));
        long drawn = topicPoolRepository.pop(roomId, null);

        topicPoolRepository.invalidate(roomId);
        topicPoolRepository.load(roomId, List.of(1L, 2L));

        assertThat(topicPoolRepository.pop(roomId, null)).isNotEqualTo(drawn).isPositive();
        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(TopicPoolRepository.EMPTY);
    }

    @Test
    @DisplayName("패스한 주제는 다시 읽어 올 때 풀에 들어간다")
    void reloadKeepsPassedTopic() {
        topicPoolRepository.load(roomId, List.of(1L, 2L));
        long drawn = topicPoolRepository.pop(roomId, null);
        topicPoolRepository.pop(roomId, drawn);

        topicPoolRepository.invalidate(roomId);
        topicPoolRepository.load(roomId, List.of(drawn));

        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(drawn);
    }

    @Test
    @DisplayName("모든 키에 TTL 이 있고, invalidate 하면 다시 읽어 와야 한다")
    void keysExpireAndInvalidateForcesReload() {
        topicPoolRepository.add(roomId, 1L);
        assertThat(redisTemplate.getExpire("topics:" + roomId + ":unused")).isPositive();

        topicPoolRepository.load(roomId, List.of(2L));
        assertThat(redisTemplate.getExpire("topics:" + roomId + ":unused-ready")).isPositive();

        topicPoolRepository.pop(roomId, null);
        assertThat(redisTemplate.getExpire("topics:" + roomId + ":drawn")).isPositive();

        topicPoolRepository.invalidate(roomId);
        assertThat(topicPoolRepository.pop(roomId, null)).isEqualTo(TopicPoolRepository.NOT_LOADED);
    }

    private boolean redisIsUp() {
        try {
            connectionFactory.getConnection().ping();
            return true;
        } catch (RuntimeException connectionError) {
            return false;
        }
    }
}
