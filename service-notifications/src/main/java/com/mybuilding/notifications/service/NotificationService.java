package com.mybuilding.notifications.service;

import com.mybuilding.incidents.event.IncidentCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final String defaultRecipient;
    private final AtomicReference<NotificationRecord> lastNotification = new AtomicReference<>();

    public NotificationService(
            JavaMailSender mailSender,
            @Value("${mybuilding.notification.default-recipient}") String defaultRecipient) {
        this.mailSender = mailSender;
        this.defaultRecipient = defaultRecipient;
    }

    public void notifyIncidentCreated(IncidentCreatedEvent event) {
        String recipient = event.recipientEmail() == null || event.recipientEmail().isBlank()
                ? defaultRecipient
                : event.recipientEmail();
        String subject = "Nouvel incident #" + event.ticketId();

        SimpleMailMessage message = new SimpleMailMessage();
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
