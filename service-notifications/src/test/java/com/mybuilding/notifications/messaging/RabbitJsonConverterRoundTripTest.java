package com.mybuilding.notifications.messaging;

import com.mybuilding.incidents.event.IncidentCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RabbitJsonConverterRoundTripTest {

    @Test
    void shouldSerializeAndDeserializeIncidentCreatedEventWithInstantAcrossServices() {
        RabbitMessagingConfig config = new RabbitMessagingConfig();
        Jackson2JsonMessageConverter producerConverter = new Jackson2JsonMessageConverter();
        Jackson2JsonMessageConverter consumerConverter = config.rabbitMessageConverter();

        Instant now = Instant.parse("2026-11-24T21:30:00Z");
        IncidentCreatedEvent event = new IncidentCreatedEvent(
                10L, 1L, "Fuite d'eau", "HAUTE", "ahmed.alami@mybuilding.ma", now
        );

        MessageProperties props = new MessageProperties();
        Message amqpMessage = producerConverter.toMessage(event, props);
        Object converted = consumerConverter.fromMessage(amqpMessage);

        assertInstanceOf(IncidentCreatedEvent.class, converted);
        IncidentCreatedEvent decoded = (IncidentCreatedEvent) converted;
        assertEquals(10L, decoded.ticketId());
        assertEquals("ahmed.alami@mybuilding.ma", decoded.recipientEmail());
        assertEquals(now, decoded.occurredAt());
    }
}
