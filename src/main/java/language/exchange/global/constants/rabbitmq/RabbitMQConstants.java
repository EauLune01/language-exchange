package language.exchange.global.constants.rabbitmq;


public interface RabbitMQConstants {

    // Topic (주제 등록 / 사용 날짜 처리 - 응답 속도를 위해 DB 반영은 비동기)
    String TOPIC_EXCHANGE = "topic.exchange";
    String TOPIC_DLX = "topic.dlx";

    String TOPIC_CREATE_QUEUE = "topic.create.queue";
    String TOPIC_CREATE_DLQ = "topic.create.dlq";
    String TOPIC_CREATE_KEY = "topic.create";

    String TOPIC_USED_QUEUE = "topic.used.queue";
    String TOPIC_USED_DLQ = "topic.used.dlq";
    String TOPIC_USED_KEY = "topic.used";
}