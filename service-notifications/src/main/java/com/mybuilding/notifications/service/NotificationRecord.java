package com.mybuilding.notifications.service;

import java.time.Instant;

public record NotificationRecord(
        Long ticketId,
        String recipient,
        String subject,
        Instant receivedAt,
        String status
) {
}
