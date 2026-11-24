package com.mybuilding.notifications.controller;

import com.mybuilding.incidents.event.IncidentCreatedEvent;
import com.mybuilding.notifications.messaging.IncidentCreatedConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@TestPropertySource(locations = "file:../config-repo/service-notifications.properties")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentCreatedConsumer consumer;

    @MockBean
    private JavaMailSenderImpl mailSender;

    @Test
    void shouldConsumeIncidentEventSendEmailAndExposeLastNotification() throws Exception {
        // Avant tout événement -> 404 format ControllerAdvice uniforme
        mockMvc.perform(get("/api/notifications/last"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Simulation réception RabbitMQ
        consumer.consume(new IncidentCreatedEvent(
                42L,
                1L,
                "Fuite ascenseur",
                "HAUTE",
                "ahmed@mybuilding.ma",
                Instant.now()
        ));

        org.mockito.ArgumentCaptor<SimpleMailMessage> captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("noreply@mybuilding.local", captor.getValue().getFrom());

        mockMvc.perform(get("/api/notifications/last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(42))
                .andExpect(jsonPath("$.recipient").value("ahmed@mybuilding.ma"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }
}
