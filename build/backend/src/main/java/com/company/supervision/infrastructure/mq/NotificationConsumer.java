package com.company.supervision.infrastructure.mq;

import com.company.supervision.infrastructure.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class NotificationConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onAuditEvent(Map<String, Object> message) {
        log.info("[MQ] 消费执行审计事件: {}", message);
    }
}
