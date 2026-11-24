package com.mybuilding.notifications.service;

import com.mybuilding.incidents.event.IncidentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String defaultRecipient;
    private final AtomicReference<NotificationRecord> lastNotification = new AtomicReference<>();

    public NotificationService(
            JavaMailSender mailSender,
            @Value("${mybuilding.notification.from:noreply@mybuilding.local}") String fromAddress,
            @Value("${mybuilding.notification.default-recipient}") String defaultRecipient) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.defaultRecipient = defaultRecipient;
    }

    public void notifyIncidentCreated(IncidentCreatedEvent event) {
        String recipient = event.recipientEmail() == null || event.recipientEmail().isBlank()
                ? defaultRecipient
                : event.recipientEmail();
        String subject = "Nouvel incident #" + event.ticketId();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText("""
                Un nouvel incident a été créé.
                Ticket : %d
                Appartement : %d
                Priorité : %s
                Description : %s
                """.formatted(
                event.ticketId(),
                event.apartmentId(),
                event.priority(),
                event.description()
        ));
        mailSender.send(message);
        log.info("[RabbitMQ] Notification email envoyée pour ticket #{} à {}", event.ticketId(), recipient);

        lastNotification.set(new NotificationRecord(
                event.ticketId(),
                recipient,
                subject,
                Instant.now(),
                "SENT"
        ));
    }

    public NotificationRecord getLastNotification() {
        return lastNotification.get();
    }
}
