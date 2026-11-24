package com.mybuilding.incidents.service.impl;

import com.mybuilding.incidents.dto.TicketRequestDTO;
import com.mybuilding.incidents.dto.TicketResponseDTO;
import com.mybuilding.incidents.dto.TicketResponseFullDTO;
import com.mybuilding.incidents.feignClient.ApartmentDTO;
import com.mybuilding.incidents.feignClient.ResidentDTO;
import com.mybuilding.incidents.service.ResidentsServiceClient;
import com.mybuilding.incidents.event.IncidentCreatedEvent;
import com.mybuilding.incidents.messaging.IncidentEventPublisher;
import com.mybuilding.incidents.mapper.IncidentMapper;
import com.mybuilding.incidents.model.StatutTicket;
import com.mybuilding.incidents.model.Ticket;
import com.mybuilding.incidents.repository.TicketRepository;
import com.mybuilding.incidents.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.Instant;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final IncidentMapper incidentMapper;
    private final ResidentsServiceClient residentsServiceClient;
    private final IncidentEventPublisher incidentEventPublisher;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             IncidentMapper incidentMapper,
                             ResidentsServiceClient residentsServiceClient,
                             IncidentEventPublisher incidentEventPublisher) {
        this.ticketRepository = ticketRepository;
        this.incidentMapper = incidentMapper;
        this.residentsServiceClient = residentsServiceClient;
        this.incidentEventPublisher = incidentEventPublisher;
    }

    @Override
    public TicketResponseDTO create(TicketRequestDTO dto) {
        // --- Jalon V4 & V6 : Validation distante avec Résilience (CircuitBreaker / Retry / Timeout) ---
        ApartmentDTO apartment = residentsServiceClient.getApartmentById(dto.getApartmentId());
        String recipientEmail = null;
        if (apartment != null && apartment.getResidentId() != null) {
            ResidentDTO resident = residentsServiceClient.getResidentById(apartment.getResidentId());
            if (resident != null) {
                recipientEmail = resident.getEmail();
            }
        }

        Ticket ticket = incidentMapper.toEntity(dto);
        Ticket saved = ticketRepository.save(ticket);
        incidentEventPublisher.publish(new IncidentCreatedEvent(
                saved.getId(),
                saved.getApartmentId(),
                saved.getDescription(),
                saved.getPriorite().name(),
                recipientEmail,
                Instant.now()
        ));
        return incidentMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getAll() {
        return ticketRepository.findAll().stream()
                .map(incidentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDTO getById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable avec l'id : " + id));
        return incidentMapper.toResponseDTO(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseFullDTO getTicketFullById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable avec l'id : " + id));

        TicketResponseFullDTO fullDto = new TicketResponseFullDTO();
        fullDto.setId(ticket.getId());
        fullDto.setApartmentId(ticket.getApartmentId());
        fullDto.setDescription(ticket.getDescription());
        fullDto.setPriorite(ticket.getPriorite());
        fullDto.setStatut(ticket.getStatut());

        // Récupération distante avec Résilience (CircuitBreaker & Fallback)
        try {
            ApartmentDTO apartment = residentsServiceClient.getApartmentById(ticket.getApartmentId());
            if (apartment != null) {
                fullDto.setApartmentNumero(apartment.getNumero());
                fullDto.setEtage(apartment.getEtage());
                fullDto.setSuperficie(apartment.getSuperficie());
                fullDto.setResidentId(apartment.getResidentId());

                if (apartment.getResidentId() != null) {
                    ResidentDTO resident = residentsServiceClient.getResidentById(apartment.getResidentId());
                    if (resident != null) {
                        fullDto.setResidentNom(resident.getNom());
                        fullDto.setResidentEmail(resident.getEmail());
                        fullDto.setResidentTelephone(resident.getTelephone());
                        fullDto.setResidentRole(resident.getRole());
                    }
                }
            }
        } catch (Exception e) {
            // Tolérance si dégradation
        }

        return fullDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getByApartmentId(Long apartmentId) {
        return ticketRepository.findByApartmentId(apartmentId).stream()
                .map(incidentMapper::toResponseDTO)
                .toList();
    }

    @Override
    public TicketResponseDTO updateStatus(Long id, StatutTicket newStatus) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable avec l'id : " + id));

        if (ticket.getStatut() == StatutTicket.RESOLU) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un ticket RESOLU ne peut plus être modifié (RM-TIC-02)");
        }
        if (ticket.getStatut() == StatutTicket.ANNULE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un ticket ANNULE ne peut plus être repris (RM-TIC-03)");
        }

        ticket.setStatut(newStatus);
        Ticket updated = ticketRepository.save(ticket);
        return incidentMapper.toResponseDTO(updated);
    }

    @Override
    public void delete(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket introuvable avec l'id : " + id);
        }
        ticketRepository.deleteById(id);
    }
}
