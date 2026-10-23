package com.contaplus.api.expense;

import com.contaplus.api.store.Store;
import com.contaplus.api.transaction.TransactionSource;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private String description;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionSource source;

    @Column(name = "original_input")
    private String originalInput;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Expense() {
    }

    public Expense(Store store, String description, Integer amountCents, ExpenseCategory category,
                   OffsetDateTime occurredAt, TransactionSource source, String originalInput,
                   UUID idempotencyKey) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.description = description;
        this.amountCents = amountCents;
        this.category = category;
        this.occurredAt = occurredAt;
        this.source = source;
        this.originalInput = originalInput;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStoreId() {
        return store.getId();
    }

    public String getDescription() {
        return description;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public TransactionSource getSource() {
        return source;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }
}
