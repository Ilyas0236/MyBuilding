package com.mybuilding.residents.controller;

import com.mybuilding.residents.dto.ResidentRequestDTO;
import com.mybuilding.residents.dto.ResidentResponseDTO;
import com.mybuilding.residents.service.ResidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/residents")
public class ResidentController {

    private final ResidentService residentService;

    public ResidentController(ResidentService residentService) {
        this.residentService = residentService;
    }

    @GetMapping
    public ResponseEntity<List<ResidentResponseDTO>> getAll() {
        return ResponseEntity.ok(residentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResidentResponseDTO> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(residentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ResidentResponseDTO> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ResidentRequestDTO dto) {
        ResidentResponseDTO created = residentService.create(dto, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResidentResponseDTO> update(@PathVariable("id") Long id, @Valid @RequestBody ResidentRequestDTO dto) {
        return ResponseEntity.ok(residentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        residentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/solde")
    public ResponseEntity<ResidentResponseDTO> updateSolde(@PathVariable("id") Long id, @RequestBody Map<String, BigDecimal> payload) {
        BigDecimal delta = payload.getOrDefault("delta", BigDecimal.ZERO);
        return ResponseEntity.ok(residentService.updateSolde(id, delta));
    }
}
