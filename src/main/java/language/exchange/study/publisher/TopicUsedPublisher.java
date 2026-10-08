package language.exchange.study.publisher;

import language.exchange.global.constants.rabbitmq.RabbitMQConstants;
import language.exchange.study.event.TopicUsedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TopicUsedPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(Long topicId, LocalDate usedDate) {
        TopicUsedEvent event = TopicUsedEvent.create(topicId, usedDate);
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.TOPIC_EXCHANGE,
                RabbitMQConstants.TOPIC_USED_KEY,
                event);
    }
}