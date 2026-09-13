package com.contaplus.api.customer;

import com.contaplus.api.store.Store;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private String name;

    @Column
    private String phone;

    @Column
    private String email;

    @Column(name = "cpf_cnpj")
    private String cpfCnpj;

    @Column
    private String notes;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Customer() {
    }

    public Customer(Store store, String name, String phone, String email, String cpfCnpj, String notes) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.cpfCnpj = cpfCnpj;
        this.notes = notes;
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

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public String getNotes() {
        return notes;
    }

    public Boolean getActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String name, String phone, String email, String cpfCnpj, String notes) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.cpfCnpj = cpfCnpj;
        this.notes = notes;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = OffsetDateTime.now();
    }
}
