package com.mybuilding.residents.idempotency;

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

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 50)
    private String resourceType;

    @Column(length = 500)
    private String responseData;

    public IdempotencyKey() {}

    public IdempotencyKey(String keyValue, String resourceType, String responseData) {
        this.keyValue = keyValue;
        this.resourceType = resourceType;
        this.responseData = responseData;
        this.createdAt = Instant.now();
    }

    public String getKeyValue() { return keyValue; }
    public void setKeyValue(String keyValue) { this.keyValue = keyValue; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResponseData() { return responseData; }
    public void setResponseData(String responseData) { this.responseData = responseData; }
}
