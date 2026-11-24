package com.mybuilding.notifications.messaging;

import com.mybuilding.incidents.event.IncidentCreatedEvent;
import com.mybuilding.notifications.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentCreatedConsumer {

    private final NotificationService notificationService;

    public IncidentCreatedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${mybuilding.messaging.queue}")
    public void consume(IncidentCreatedEvent event) {
        notificationService.notifyIncidentCreated(event);
    }
}
