package com.company.supervision.infrastructure.mq;

import com.company.supervision.infrastructure.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 异步发布一次执行完成的审计事件（解耦执行与通知）。
     */
    public void publishAudit(Long taskId, Long executionId, int status) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("taskId", taskId);
        msg.put("executionId", executionId);
        msg.put("status", status);
        msg.put("time", LocalDateTime.now().toString());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, msg);
        log.info("[MQ] 发布执行审计事件 taskId={} executionId={}", taskId, executionId);
    }
}
