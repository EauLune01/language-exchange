package language.exchange.global.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TOPIC_EXCHANGE = "topic.exchange";

    public static final String TOPIC_USED_QUEUE = "topic.used.queue";
    public static final String TOPIC_USED_ROUTING_KEY = "topic.used";

    public static final String TOPIC_CREATE_QUEUE = "topic.create.queue";
    public static final String TOPIC_CREATE_ROUTING_KEY = "topic.create";

    public static final String TOPIC_DEAD_LETTER_EXCHANGE = "topic.dlx";
    public static final String TOPIC_CREATE_DEAD_LETTER_QUEUE = "topic.create.dlq";
    public static final String TOPIC_CREATE_DEAD_LETTER_ROUTING_KEY = "topic.create.dead";

    @Bean
    public DirectExchange topicExchange() {
        return new DirectExchange(TOPIC_EXCHANGE);
    }

    @Bean
    public DirectExchange topicDeadLetterExchange() {
        return new DirectExchange(TOPIC_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue topicUsedQueue() {
        return QueueBuilder.durable(TOPIC_USED_QUEUE).build();
    }

    @Bean
    public Binding topicUsedBinding() {
        return BindingBuilder.bind(topicUsedQueue())
                .to(topicExchange())
                .with(TOPIC_USED_ROUTING_KEY);
    }

    @Bean
    public Queue topicCreateQueue() {
        return QueueBuilder.durable(TOPIC_CREATE_QUEUE)
                .deadLetterExchange(TOPIC_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(TOPIC_CREATE_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding topicCreateBinding() {
        return BindingBuilder.bind(topicCreateQueue())
                .to(topicExchange())
                .with(TOPIC_CREATE_ROUTING_KEY);
    }

    @Bean
    public Queue topicCreateDeadLetterQueue() {
        return QueueBuilder.durable(TOPIC_CREATE_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding topicCreateDeadLetterBinding() {
        return BindingBuilder.bind(topicCreateDeadLetterQueue())
                .to(topicDeadLetterExchange())
                .with(TOPIC_CREATE_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}