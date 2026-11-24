package com.mybuilding.finances.service;

import com.mybuilding.finances.feignClient.ApartmentDTO;
import com.mybuilding.finances.feignClient.ResidentDTO;
import com.mybuilding.finances.feignClient.ResidentsFeignClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResidentsServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ResidentsServiceClient.class);
    private final ResidentsFeignClient residentsFeignClient;

    public ResidentsServiceClient(ResidentsFeignClient residentsFeignClient) {
        this.residentsFeignClient = residentsFeignClient;
    }

    @CircuitBreaker(name = "residentsService", fallbackMethod = "fallbackGetApartment")
    @Retry(name = "residentsService")
    public ApartmentDTO getApartmentById(Long id) {
        return residentsFeignClient.getApartmentById(id);
    }

    @CircuitBreaker(name = "residentsService", fallbackMethod = "fallbackGetResident")
    @Retry(name = "residentsService")
    public ResidentDTO getResidentById(Long id) {
        return residentsFeignClient.getResidentById(id);
    }

    public ApartmentDTO fallbackGetApartment(Long id, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound || (throwable.getCause() instanceof FeignException.NotFound)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'appartement spécifié n'existe pas : " + id);
        }
        log.warn("Fallback de résilience déclenché pour getApartmentById({}): {}", id, throwable.getMessage());
        ApartmentDTO fallback = new ApartmentDTO();
        fallback.setId(id);
        fallback.setNumero("INDISPONIBLE (CircuitBreaker)");
        fallback.setEtage(0);
        return fallback;
    }

    public ResidentDTO fallbackGetResident(Long id, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound || (throwable.getCause() instanceof FeignException.NotFound)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Le résident spécifié n'existe pas : " + id);
        }
        log.warn("Fallback de résilience déclenché pour getResidentById({}): {}", id, throwable.getMessage());
        ResidentDTO fallback = new ResidentDTO();
        fallback.setId(id);
        fallback.setNom("Service Résidents Indisponible");
        fallback.setEmail("indisponible@mybuilding.local");
        return fallback;
    }
}
