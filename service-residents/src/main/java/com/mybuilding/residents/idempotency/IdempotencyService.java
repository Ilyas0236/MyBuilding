package com.mybuilding.residents.idempotency;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    public void validateAndRegister(String key, String resourceType) {
        if (key == null || key.trim().isEmpty()) {
            return; // Optionnel si non fourni
        }
        if (repository.existsById(key)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Opération déjà traitée avec la clé d'idempotence : " + key);
        }
        repository.save(new IdempotencyKey(key, resourceType, "PROCESSED"));
    }
}
