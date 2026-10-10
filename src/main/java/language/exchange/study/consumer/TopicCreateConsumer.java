package language.exchange.study.consumer;

import language.exchange.global.constants.rabbitmq.RabbitMQConstants;
import language.exchange.study.event.TopicCreateEvent;
import language.exchange.study.repository.TopicPoolRepository;
import language.exchange.study.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicCreateConsumer {

    private final TopicService topicService;
    private final TopicPoolRepository topicPoolRepository;

    @RabbitListener(queues = RabbitMQConstants.TOPIC_CREATE_QUEUE)
    public void consume(TopicCreateEvent event) {
        log.info("[TopicCreateConsumer] create topic - roomId: {}", event.getRoomId());
        Long topicId = topicService.createTopic(event.getRoomId(), event.toCommand());
        try {
            topicPoolRepository.add(event.getRoomId(), topicId);
        } catch (DataAccessException redisError) {
            // 여기서 예외를 던지면 재시도가 같은 주제를 한 번 더 만든다. 풀에서 빠진 주제는 풀이 비었을 때 DB 에서 다시 읽힌다 (UnusedTopicPicker)
            log.warn("[TopicCreateConsumer] failed to add topic to pool - topicId: {}, cause: {}", topicId, redisError.getMessage());
        }
    }
}
