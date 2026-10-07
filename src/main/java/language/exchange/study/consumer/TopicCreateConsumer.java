package language.exchange.study.consumer;

import language.exchange.global.config.RabbitMQConfig;
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

    @RabbitListener(queues = RabbitMQConfig.TOPIC_CREATE_QUEUE)
    public void consume(TopicCreateEvent event) {
        log.info("주제 등록 처리 - nameKo: {}", event.getNameKo());
        topicService.createTopic(event.toCommand());
    }
}