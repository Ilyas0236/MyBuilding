package com.mybuilding.finances.controller;

import com.mybuilding.finances.dto.ChargeRequestDTO;
import com.mybuilding.finances.dto.ChargeResponseDTO;
import com.mybuilding.finances.dto.ChargeResponseFullDTO;
import com.mybuilding.finances.service.ChargeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/charges")
public class ChargeController {

    private final ChargeService chargeService;

    public ChargeController(ChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @GetMapping
    public ResponseEntity<List<ChargeResponseDTO>> getAll() {
        return ResponseEntity.ok(chargeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChargeResponseDTO> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(chargeService.getById(id));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ChargeResponseFullDTO> getDetailById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(chargeService.getChargeFullById(id));
    }

    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<ChargeResponseDTO>> getByApartment(@PathVariable("apartmentId") Long apartmentId) {
        return ResponseEntity.ok(chargeService.getByApartmentId(apartmentId));
    }

    @GetMapping("/unpaid")
    public ResponseEntity<List<ChargeResponseDTO>> getUnpaid() {
        return ResponseEntity.ok(chargeService.getUnpaid());
    }

    @PostMapping
    public ResponseEntity<ChargeResponseDTO> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ChargeRequestDTO dto) {
        ChargeResponseDTO created = chargeService.create(dto, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        chargeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
