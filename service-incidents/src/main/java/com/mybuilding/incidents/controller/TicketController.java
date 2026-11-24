package com.mybuilding.incidents.controller;

import com.mybuilding.incidents.dto.TicketRequestDTO;
import com.mybuilding.incidents.dto.TicketResponseDTO;
import com.mybuilding.incidents.dto.TicketResponseFullDTO;
import com.mybuilding.incidents.model.StatutTicket;
import com.mybuilding.incidents.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> getAll() {
        return ResponseEntity.ok(ticketService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<TicketResponseFullDTO> getDetailById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ticketService.getTicketFullById(id));
    }

    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<TicketResponseDTO>> getByApartment(@PathVariable("apartmentId") Long apartmentId) {
        return ResponseEntity.ok(ticketService.getByApartmentId(apartmentId));
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TicketRequestDTO dto) {
        TicketResponseDTO created = ticketService.create(dto, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponseDTO> updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> payload) {
        StatutTicket statut = StatutTicket.valueOf(payload.get("statut"));
        return ResponseEntity.ok(ticketService.updateStatus(id, statut));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
