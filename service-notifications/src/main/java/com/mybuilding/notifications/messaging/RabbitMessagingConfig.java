package com.mybuilding.notifications.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

    @Bean
    public DirectExchange myBuildingEventsExchange(
            @Value("${mybuilding.messaging.exchange}") String exchange) {
        return ExchangeBuilder.directExchange(exchange).durable(true).build();
    }

    @Bean
    public Queue incidentCreatedQueue(@Value("${mybuilding.messaging.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding incidentCreatedBinding(
            Queue incidentCreatedQueue,
            DirectExchange myBuildingEventsExchange,
            @Value("${mybuilding.messaging.routing-key}") String routingKey) {
        return BindingBuilder.bind(incidentCreatedQueue)
                .to(myBuildingEventsExchange)
                .with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
