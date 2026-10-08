package language.exchange.study.publisher;

import language.exchange.global.constants.rabbitmq.RabbitMQConstants;
import language.exchange.study.event.TopicCreateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TopicCreatePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(TopicCreateEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConstants.TOPIC_EXCHANGE,
                RabbitMQConstants.TOPIC_CREATE_KEY,
                event);
    }
}