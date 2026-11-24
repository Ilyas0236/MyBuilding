package com.mybuilding.notifications.controller;

import com.mybuilding.notifications.service.NotificationRecord;
import com.mybuilding.notifications.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/last")
    public ResponseEntity<NotificationRecord> getLastNotification() {
        NotificationRecord notification = notificationService.getLastNotification();
        if (notification == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune notification reçue pour le moment");
        }
        return ResponseEntity.ok(notification);
    }
}
