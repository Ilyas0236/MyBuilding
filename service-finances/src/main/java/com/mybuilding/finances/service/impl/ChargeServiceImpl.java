package com.mybuilding.finances.service.impl;

import com.mybuilding.finances.dto.ChargeRequestDTO;
import com.mybuilding.finances.dto.ChargeResponseDTO;
import com.mybuilding.finances.dto.ChargeResponseFullDTO;
import com.mybuilding.finances.feignClient.ApartmentDTO;
import com.mybuilding.finances.feignClient.ResidentDTO;
import com.mybuilding.finances.idempotency.IdempotencyService;
import com.mybuilding.finances.service.ResidentsServiceClient;
import com.mybuilding.finances.mapper.FinanceMapper;
import com.mybuilding.finances.model.Charge;
import com.mybuilding.finances.model.StatutCharge;
import com.mybuilding.finances.repository.ChargeRepository;
import com.mybuilding.finances.service.ChargeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ChargeServiceImpl implements ChargeService {

    private static final Logger log = LoggerFactory.getLogger(ChargeServiceImpl.class);

    private final ChargeRepository chargeRepository;
    private final FinanceMapper financeMapper;
    private final ResidentsServiceClient residentsServiceClient;
    private final IdempotencyService idempotencyService;

    public ChargeServiceImpl(ChargeRepository chargeRepository,
                             FinanceMapper financeMapper,
                             ResidentsServiceClient residentsServiceClient,
                             IdempotencyService idempotencyService) {
        this.chargeRepository = chargeRepository;
        this.financeMapper = financeMapper;
        this.residentsServiceClient = residentsServiceClient;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public ChargeResponseDTO create(ChargeRequestDTO dto) {
        return create(dto, null);
    }

    @Override
    public ChargeResponseDTO create(ChargeRequestDTO dto, String idempotencyKey) {
        idempotencyService.ensureNotProcessed(idempotencyKey);

        // --- Jalon V4 & V6 : Validation distante avec Résilience (404 propagé, fallback => 503) ---
        ApartmentDTO apartment = residentsServiceClient.getApartmentById(dto.getApartmentId());
        if (ResidentsServiceClient.isFallbackApartment(apartment)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Service Résidents temporairement indisponible : validation de l'appartement impossible");
        }

        if (chargeRepository.existsByApartmentIdAndPeriode(dto.getApartmentId(), dto.getPeriode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une charge a déjà été émise pour l'appartement " + dto.getApartmentId() + " sur la période " + dto.getPeriode() + " (RM-CHG-04)");
        }

        Charge charge = financeMapper.toEntity(dto);
        Charge saved = chargeRepository.save(charge);
        idempotencyService.register(idempotencyKey, "CHARGE");
        return financeMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargeResponseDTO> getAll() {
        return chargeRepository.findAll().stream()
                .map(financeMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChargeResponseDTO getById(Long id) {
        Charge charge = chargeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charge introuvable avec l'id : " + id));
        return financeMapper.toResponseDTO(charge);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargeResponseDTO> getByApartmentId(Long apartmentId) {
        return chargeRepository.findByApartmentId(apartmentId).stream()
                .map(financeMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargeResponseDTO> getUnpaid() {
        return chargeRepository.findAll().stream()
                .filter(c -> c.getStatut() != StatutCharge.PAYEE)
                .map(financeMapper::toResponseDTO)
                .toList();
    }

    @Override
    public void delete(Long id) {
        Charge charge = chargeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charge introuvable avec l'id : " + id));
        chargeRepository.delete(charge);
    }

    @Override
    @Transactional(readOnly = true)
    public ChargeResponseFullDTO getChargeFullById(Long id) {
        Charge charge = chargeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charge introuvable avec l'id : " + id));

        ChargeResponseFullDTO fullDto = new ChargeResponseFullDTO();
        fullDto.setId(charge.getId());
        fullDto.setApartmentId(charge.getApartmentId());
        fullDto.setPeriode(charge.getPeriode());
        fullDto.setMontant(charge.getMontant());
        fullDto.setMontantPaye(charge.getMontantPaye());
        fullDto.setStatut(charge.getStatut());

        // Récupération distante avec Résilience (CircuitBreaker & Fallback)
        try {
            ApartmentDTO apartment = residentsServiceClient.getApartmentById(charge.getApartmentId());
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
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Dégradation lors de l'enrichissement de la charge #{}: {}", id, e.getMessage());
        }

        return fullDto;
    }
}
