package com.mybuilding.finances.controller;

import com.mybuilding.finances.dto.PaymentRequestDTO;
import com.mybuilding.finances.dto.PaymentResponseDTO;
import com.mybuilding.finances.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> getAll() {
        return ResponseEntity.ok(paymentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/charge/{chargeId}")
    public ResponseEntity<List<PaymentResponseDTO>> getByCharge(@PathVariable("chargeId") Long chargeId) {
        return ResponseEntity.ok(paymentService.getByChargeId(chargeId));
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> recordPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequestDTO dto) {
        PaymentResponseDTO created = paymentService.recordPayment(dto, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
