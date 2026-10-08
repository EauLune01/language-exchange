package language.exchange.global.config.rabbitmq;

import language.exchange.global.constants.rabbitmq.RabbitMQConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange topicExchange() {
        return new DirectExchange(RabbitMQConstants.TOPIC_EXCHANGE);
    }

    @Bean
    public DirectExchange topicDlx() {
        return new DirectExchange(RabbitMQConstants.TOPIC_DLX);
    }

    // ---------- 주제 등록 ----------

    @Bean
    public Queue topicCreateQueue() {
        return QueueBuilder.durable(RabbitMQConstants.TOPIC_CREATE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.TOPIC_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.TOPIC_CREATE_DLQ)
                .build();
    }

    @Bean
    public Queue topicCreateDlq() {
        return QueueBuilder.durable(RabbitMQConstants.TOPIC_CREATE_DLQ).build();
    }

    @Bean
    public Binding topicCreateBinding(Queue topicCreateQueue, DirectExchange topicExchange) {
        return BindingBuilder.bind(topicCreateQueue)
                .to(topicExchange)
                .with(RabbitMQConstants.TOPIC_CREATE_KEY);
    }

    @Bean
    public Binding topicCreateDlqBinding(Queue topicCreateDlq, DirectExchange topicDlx) {
        return BindingBuilder.bind(topicCreateDlq)
                .to(topicDlx)
                .with(RabbitMQConstants.TOPIC_CREATE_DLQ);
    }

    // ---------- 주제 사용 날짜 처리 ----------

    @Bean
    public Queue topicUsedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.TOPIC_USED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.TOPIC_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.TOPIC_USED_DLQ)
                .build();
    }

    @Bean
    public Queue topicUsedDlq() {
        return QueueBuilder.durable(RabbitMQConstants.TOPIC_USED_DLQ).build();
    }

    @Bean
    public Binding topicUsedBinding(Queue topicUsedQueue, DirectExchange topicExchange) {
        return BindingBuilder.bind(topicUsedQueue)
                .to(topicExchange)
                .with(RabbitMQConstants.TOPIC_USED_KEY);
    }

    @Bean
    public Binding topicUsedDlqBinding(Queue topicUsedDlq, DirectExchange topicDlx) {
        return BindingBuilder.bind(topicUsedDlq)
                .to(topicDlx)
                .with(RabbitMQConstants.TOPIC_USED_DLQ);
    }

    // ---------- 직렬화 ----------

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}