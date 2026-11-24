package com.mybuilding.incidents.event;

import java.time.Instant;

public record IncidentCreatedEvent(
        Long ticketId,
        Long apartmentId,
        String description,
        String priority,
        String recipientEmail,
        Instant occurredAt
) {
}
