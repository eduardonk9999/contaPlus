package com.contaplus.api.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stores")
public class Store {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Store() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
}
