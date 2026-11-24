package com.mybuilding.incidents.service;

import com.mybuilding.incidents.feignClient.ApartmentDTO;
import com.mybuilding.incidents.feignClient.ResidentDTO;
import com.mybuilding.incidents.feignClient.ResidentsFeignClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResidentsServiceClient {

    public static final String UNAVAILABLE_APARTMENT = "INDISPONIBLE (CircuitBreaker)";
    public static final String UNAVAILABLE_EMAIL = "indisponible@mybuilding.local";

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

    public static boolean isFallbackApartment(ApartmentDTO apartment) {
        return apartment == null || UNAVAILABLE_APARTMENT.equals(apartment.getNumero());
    }

    public static boolean isFallbackResident(ResidentDTO resident) {
        return resident == null || UNAVAILABLE_EMAIL.equals(resident.getEmail());
    }

    public ApartmentDTO fallbackGetApartment(Long id, Throwable throwable) {
        rethrowIfNotFound(throwable);
        log.warn("Fallback déclenché pour getApartmentById({}): {}", id, throwable.getMessage());
        ApartmentDTO fallback = new ApartmentDTO();
        fallback.setId(id);
        fallback.setNumero(UNAVAILABLE_APARTMENT);
        fallback.setEtage(0);
        return fallback;
    }

    public ResidentDTO fallbackGetResident(Long id, Throwable throwable) {
        rethrowIfNotFound(throwable);
        log.warn("Fallback déclenché pour getResidentById({}): {}", id, throwable.getMessage());
        ResidentDTO fallback = new ResidentDTO();
        fallback.setId(id);
        fallback.setNom("Service Résidents Indisponible");
        fallback.setEmail(UNAVAILABLE_EMAIL);
        return fallback;
    }

    private static void rethrowIfNotFound(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof FeignException.NotFound notFound) {
                throw notFound;
            }
            current = current.getCause();
        }
    }
}
