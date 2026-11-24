package com.mybuilding.incidents.messaging;

import com.mybuilding.incidents.event.IncidentCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class IncidentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public IncidentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(IncidentCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMessagingConfig.EXCHANGE,
                RabbitMessagingConfig.INCIDENT_CREATED_ROUTING_KEY,
                event
        );
    }
}
