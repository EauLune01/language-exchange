package language.exchange.study.consumer;

import language.exchange.global.constants.rabbitmq.RabbitMQConstants;
import language.exchange.study.event.TopicCreateEvent;
import language.exchange.study.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicCreateConsumer {

    private final TopicService topicService;

    @RabbitListener(queues = RabbitMQConstants.TOPIC_CREATE_QUEUE)
    public void consume(TopicCreateEvent event) {
        log.info("[TopicCreateConsumer] nameKo={}", event.getNameKo());
        topicService.createTopic(event.toCommand());
    }
}