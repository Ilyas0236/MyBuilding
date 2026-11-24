package com.mybuilding.finances.service.impl;

import com.mybuilding.finances.dto.PaymentRequestDTO;
import com.mybuilding.finances.feignClient.ResidentsFeignClient;
import com.mybuilding.finances.idempotency.IdempotencyKeyRepository;
import com.mybuilding.finances.model.MoyenPaiement;
import com.mybuilding.finances.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class PaymentIdempotencyIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @MockBean
    private ResidentsFeignClient residentsFeignClient;

    @Test
    void shouldRollbackIdempotencyKeyOnFailedPaymentThenBlockReplayAfterSuccess() {
        String key = "TEST-IDEMP-PAY-001";

        // 1. Paiement invalide (dépasse le reste dû de 1000.00 sur charge 1) -> doit échouer sans brûler la clé
        PaymentRequestDTO invalidDto = new PaymentRequestDTO(1L, LocalDate.now(), new BigDecimal("50000.00"), MoyenPaiement.VIREMENT);
        ResponseStatusException badReq = assertThrows(ResponseStatusException.class,
                () -> paymentService.recordPayment(invalidDto, key));
        assertEquals(HttpStatus.BAD_REQUEST, badReq.getStatusCode());
        assertFalse(idempotencyKeyRepository.existsById(key), "La clé ne doit pas être enregistrée si le paiement échoue");

        // 2. Paiement valide avec la même clé -> doit réussir (201) et enregistrer la clé
        PaymentRequestDTO validDto = new PaymentRequestDTO(1L, LocalDate.now(), new BigDecimal("100.00"), MoyenPaiement.VIREMENT);
        assertNotNull(paymentService.recordPayment(validDto, key).getId());
        assertTrue(idempotencyKeyRepository.existsById(key));

        // 3. Rejeu avec la même clé -> 409 Conflict
        ResponseStatusException conflict = assertThrows(ResponseStatusException.class,
                () -> paymentService.recordPayment(validDto, key));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
    }
}
