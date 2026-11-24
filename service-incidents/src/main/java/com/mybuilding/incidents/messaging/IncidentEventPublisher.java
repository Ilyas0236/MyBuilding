package com.mybuilding.incidents.messaging;

import com.mybuilding.incidents.event.IncidentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class IncidentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public IncidentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(IncidentCreatedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMessagingConfig.EXCHANGE,
                    RabbitMessagingConfig.INCIDENT_CREATED_ROUTING_KEY,
                    event
            );
        } catch (AmqpException ex) {
            log.warn("Broker RabbitMQ indisponible, événement IncidentCreatedEvent non publié pour ticket #{}: {}",
                    event.ticketId(), ex.getMessage());
        }
    }
}
