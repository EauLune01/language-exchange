package language.exchange.global.constants.redis;


public interface RedisKeyConstants {

    // Topic (방마다 아직 안 쓴 주제 id 의 Set / 그 Set 을 DB 에서 읽어 왔다는 표시 / 방금 뽑힌 주제 id 의 Set) - %s 는 roomId
    String TOPIC_UNUSED_POOL = "topics:%s:unused";
    String TOPIC_UNUSED_READY = "topics:%s:unused-ready";
    String TOPIC_DRAWN = "topics:%s:drawn";

    static String format(String pattern, Object... args) {
        return String.format(pattern, args);
    }
}
