package language.exchange.study.consumer;

import language.exchange.global.constants.rabbitmq.RabbitMQConstants;
import language.exchange.study.event.TopicUsedEvent;
import language.exchange.study.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicUsedConsumer {

    private final TopicService topicService;

    @RabbitListener(queues = RabbitMQConstants.TOPIC_USED_QUEUE)
    public void consume(TopicUsedEvent event) {
        log.info("[TopicUsedConsumer] topicId={}, usedDate={}", event.getTopicId(), event.getUsedDate());
        topicService.markAsUsed(event.getTopicId(), event.getUsedDate());
    }
}
