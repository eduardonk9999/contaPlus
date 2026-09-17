package com.contaplus.api.category;

import com.contaplus.api.store.Store;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"store_id", "name"})
})
public class Category {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 100)
    private String name;

    @Column
    private String description;

    @Column(length = 7)
    private String color;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Category() {
    }

    public Category(Store store, String name, String description, String color) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.name = name;
        this.description = description;
        this.color = color;
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

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
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

    public void update(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = OffsetDateTime.now();
    }
}
