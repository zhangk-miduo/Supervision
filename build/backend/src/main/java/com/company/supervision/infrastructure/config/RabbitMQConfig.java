package com.company.supervision.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "supervision.exchange";
    public static final String QUEUE = "supervision.notification.queue";
    public static final String ROUTING_KEY = "supervision.wechat.send";

    @Bean
    public DirectExchange supervisionExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange supervisionExchange) {
        return BindingBuilder.bind(notificationQueue).to(supervisionExchange).with(ROUTING_KEY);
    }
}
