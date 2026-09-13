package com.contaplus.api.cashregister;

import com.contaplus.api.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cash_movements")
public class CashMovement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegister cashRegister;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashMovementType type;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected CashMovement() {
    }

    public CashMovement(CashRegister cashRegister, User user, CashMovementType type,
                        Integer amountCents, String reason) {
        this.id = UUID.randomUUID();
        this.cashRegister = cashRegister;
        this.user = user;
        this.type = type;
        this.amountCents = amountCents;
        this.reason = reason;
        this.occurredAt = OffsetDateTime.now();
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public CashRegister getCashRegister() {
        return cashRegister;
    }

    public UUID getCashRegisterId() {
        return cashRegister.getId();
    }

    public User getUser() {
        return user;
    }

    public UUID getUserId() {
        return user.getId();
    }

    public CashMovementType getType() {
        return type;
    }

    public Integer getAmountCents() {
        return amountCents;
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
