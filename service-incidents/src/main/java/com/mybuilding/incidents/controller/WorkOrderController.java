package com.mybuilding.incidents.controller;

import com.mybuilding.incidents.dto.WorkOrderRequestDTO;
import com.mybuilding.incidents.dto.WorkOrderResponseDTO;
import com.mybuilding.incidents.dto.WorkOrderResponseFullDTO;
import com.mybuilding.incidents.model.StatutWorkOrder;
import com.mybuilding.incidents.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public ResponseEntity<List<WorkOrderResponseDTO>> getAll() {
        return ResponseEntity.ok(workOrderService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponseDTO> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(workOrderService.getById(id));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<WorkOrderResponseFullDTO> getDetailById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(workOrderService.getWorkOrderFullById(id));
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<WorkOrderResponseDTO>> getByTicket(@PathVariable("ticketId") Long ticketId) {
        return ResponseEntity.ok(workOrderService.getByTicketId(ticketId));
    }

    @PostMapping
    public ResponseEntity<WorkOrderResponseDTO> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WorkOrderRequestDTO dto) {
        WorkOrderResponseDTO created = workOrderService.create(dto, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WorkOrderResponseDTO> updateStatus(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> payload) {
        String rawStatut = payload != null ? payload.get("statut") : null;
        if (rawStatut == null || rawStatut.isBlank()) {
            throw new IllegalArgumentException("Le champ 'statut' est obligatoire");
        }
        StatutWorkOrder statut = StatutWorkOrder.valueOf(rawStatut);
        return ResponseEntity.ok(workOrderService.updateStatus(id, statut));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<WorkOrderResponseDTO> close(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, BigDecimal> payload) {
        BigDecimal finalCost = payload != null ? payload.get("cout") : null;
        return ResponseEntity.ok(workOrderService.close(id, finalCost));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        workOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
