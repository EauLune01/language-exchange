package language.exchange.study.publisher;

import language.exchange.global.config.RabbitMQConfig;
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
                RabbitMQConfig.TOPIC_EXCHANGE,
                RabbitMQConfig.TOPIC_CREATE_ROUTING_KEY,
                event);
    }
}