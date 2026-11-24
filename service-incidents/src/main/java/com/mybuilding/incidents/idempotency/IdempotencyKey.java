package com.mybuilding.incidents.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(nullable = false, unique = true, length = 120)
    private String keyValue;

    @Column(nullable = false, length = 50)
    private String resourceType;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 30)
    private String status;

    public IdempotencyKey() {
    }

    public IdempotencyKey(String keyValue, String resourceType, String status) {
        this.keyValue = keyValue;
        this.resourceType = resourceType;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
