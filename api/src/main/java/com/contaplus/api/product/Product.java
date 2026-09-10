package com.contaplus.api.product;

import com.contaplus.api.store.Store;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;

    @Column(name = "cost_price_cents", nullable = false)
    private Integer costPriceCents;

    @Column(name = "sale_price_cents", nullable = false)
    private Integer salePriceCents;

    @Column(name = "stock_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_unit", nullable = false)
    private StockUnit stockUnit;

    @Column(name = "min_stock_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal minStockQuantity;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Product() {
    }

    public Product(Store store, String name, ProductType type, Integer costPriceCents, Integer salePriceCents, StockUnit stockUnit) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.name = name;
        this.type = type;
        this.costPriceCents = costPriceCents;
        this.salePriceCents = salePriceCents;
        this.stockQuantity = BigDecimal.ZERO;
        this.stockUnit = stockUnit;
        this.minStockQuantity = BigDecimal.ZERO;
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

    public ProductType getType() {
        return type;
    }

    public Integer getCostPriceCents() {
        return costPriceCents;
    }

    public Integer getSalePriceCents() {
        return salePriceCents;
    }

    public BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    public StockUnit getStockUnit() {
        return stockUnit;
    }

    public BigDecimal getMinStockQuantity() {
        return minStockQuantity;
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

    public void update(String name, ProductType type, Integer costPriceCents, Integer salePriceCents,
                       StockUnit stockUnit, BigDecimal minStockQuantity) {
        this.name = name;
        this.type = type;
        this.costPriceCents = costPriceCents;
        this.salePriceCents = salePriceCents;
        this.stockUnit = stockUnit;
        this.minStockQuantity = minStockQuantity;
        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = OffsetDateTime.now();
    }

    public void adjustStock(BigDecimal delta) {
        this.stockQuantity = this.stockQuantity.add(delta);
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isLowStock() {
        return stockQuantity.compareTo(minStockQuantity) < 0;
    }
}
