package com.mybuilding.finances.controller;

import com.mybuilding.finances.dto.PaymentRequestDTO;
import com.mybuilding.finances.dto.PaymentResponseDTO;
import com.mybuilding.finances.idempotency.IdempotencyService;
import com.mybuilding.finances.model.MoyenPaiement;
import com.mybuilding.finances.repository.PaymentRepository;
import com.mybuilding.finances.service.PaymentService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;

    public PaymentController(PaymentService paymentService, PaymentRepository paymentRepository, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.idempotencyService = idempotencyService;
    }

    @PostConstruct
    public void initData() {
        if (paymentRepository.count() == 0) {
            paymentService.recordPayment(new PaymentRequestDTO(1L, LocalDate.now(), new BigDecimal("500.00"), MoyenPaiement.VIREMENT));
        }
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
        idempotencyService.validateAndRegister(idempotencyKey, "PAYMENT");
        PaymentResponseDTO created = paymentService.recordPayment(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
