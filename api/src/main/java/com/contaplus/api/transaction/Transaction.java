package com.contaplus.api.transaction;

import com.contaplus.api.store.Store;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionSource source;

    @Column(nullable = false)
    private String description;

    @Column(name = "total_amount_cents", nullable = false)
    private Integer totalAmountCents;

    @Column(name = "total_cost_cents", nullable = false)
    private Integer totalCostCents;

    @Column(name = "gross_profit_cents", nullable = false)
    private Integer grossProfitCents;

    @Column(name = "margin_percent", precision = 5, scale = 2)
    private BigDecimal marginPercent;

    @Column(name = "original_input")
    private String originalInput;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionItem> items = new ArrayList<>();

    protected Transaction() {
    }

    public Transaction(Store store, TransactionType type, TransactionSource source,
                       String description, UUID idempotencyKey, OffsetDateTime occurredAt) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.type = type;
        this.status = TransactionStatus.CONFIRMED;
        this.source = source;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.occurredAt = occurredAt;
        this.totalAmountCents = 0;
        this.totalCostCents = 0;
        this.grossProfitCents = 0;
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

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public TransactionSource getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    public Integer getTotalAmountCents() {
        return totalAmountCents;
    }

    public Integer getTotalCostCents() {
        return totalCostCents;
    }

    public Integer getGrossProfitCents() {
        return grossProfitCents;
    }

    public BigDecimal getMarginPercent() {
        return marginPercent;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<TransactionItem> getItems() {
        return items;
    }

    public void addItem(TransactionItem item) {
        items.add(item);
        recalculateTotals();
    }

    public void setOriginalInput(String originalInput) {
        this.originalInput = originalInput;
    }

    private void recalculateTotals() {
        this.totalAmountCents = items.stream()
            .mapToInt(TransactionItem::getTotalAmountCents)
            .sum();
        this.totalCostCents = items.stream()
            .mapToInt(TransactionItem::getTotalCostCents)
            .sum();
        this.grossProfitCents = this.totalAmountCents - this.totalCostCents;

        if (this.totalAmountCents > 0) {
            this.marginPercent = BigDecimal.valueOf(this.grossProfitCents)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(this.totalAmountCents), 2, java.math.RoundingMode.HALF_UP);
        } else {
            this.marginPercent = BigDecimal.ZERO;
        }

        this.updatedAt = OffsetDateTime.now();
    }

    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
    }
}
