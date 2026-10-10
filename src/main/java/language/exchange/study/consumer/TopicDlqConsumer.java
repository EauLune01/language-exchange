package language.exchange.study.consumer;

import language.exchange.global.constants.rabbitmq.RabbitMQConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 3회 시도에도 실패해 DLQ 로 온 메시지를 로그로 남깁니다. 다시 처리하지는 않습니다.
 * (이미 같은 처리를 3번 실패한 메시지라 바로 다시 해도 실패합니다. 원인을 고친 뒤 로그의 본문으로 다시 요청합니다.)
 * 메시지는 여기서 꺼내면 큐에서 사라지므로 본문을 전부 남깁니다. 실패 원인(예외)은 이 로그 바로 앞의 Consumer 재시도 로그에 있습니다.
 */
@Slf4j
@Component
public class TopicDlqConsumer {

    @RabbitListener(queues = {RabbitMQConstants.TOPIC_CREATE_DLQ, RabbitMQConstants.TOPIC_USED_DLQ})
    public void consume(Message message) {
        log.error("[TopicDlqConsumer] dead letter - queue: {}, body: {}",
                message.getMessageProperties().getConsumerQueue(),
                new String(message.getBody(), StandardCharsets.UTF_8));
    }
}
