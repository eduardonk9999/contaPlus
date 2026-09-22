package com.contaplus.api.payment;

import com.contaplus.api.store.Store;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentMethodType type;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "accepts_change", nullable = false)
    private Boolean acceptsChange;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PaymentMethod() {
    }

    public PaymentMethod(Store store, String name, PaymentMethodType type, boolean acceptsChange) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.name = name;
        this.type = type;
        this.acceptsChange = acceptsChange;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public UUID getStoreId() {
        return store.getId();
    }

    public String getName() {
        return name;
    }

    public PaymentMethodType getType() {
        return type;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getAcceptsChange() {
        return acceptsChange;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, PaymentMethodType type, boolean acceptsChange) {
        this.name = name;
        this.type = type;
        this.acceptsChange = acceptsChange;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = OffsetDateTime.now();
    }
}
