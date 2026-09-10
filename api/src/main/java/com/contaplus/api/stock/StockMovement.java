package com.contaplus.api.stock;

import com.contaplus.api.product.Product;
import com.contaplus.api.store.Store;
import com.contaplus.api.transaction.Transaction;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType type;

    @Column(name = "quantity_delta", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityDelta;

    @Column(name = "quantity_before", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityAfter;

    @Column
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected StockMovement() {
    }

    public StockMovement(Store store, Product product, Transaction transaction,
                         StockMovementType type, BigDecimal quantityDelta,
                         BigDecimal quantityBefore, String reason, OffsetDateTime occurredAt) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.product = product;
        this.transaction = transaction;
        this.type = type;
        this.quantityDelta = quantityDelta;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityBefore.add(quantityDelta);
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.createdAt = OffsetDateTime.now();
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

    public Product getProduct() {
        return product;
    }

    public UUID getProductId() {
        return product.getId();
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public UUID getTransactionId() {
        return transaction != null ? transaction.getId() : null;
    }

    public StockMovementType getType() {
        return type;
    }

    public BigDecimal getQuantityDelta() {
        return quantityDelta;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
