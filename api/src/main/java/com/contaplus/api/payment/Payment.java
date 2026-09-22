package com.contaplus.api.payment;

import com.contaplus.api.transaction.Transaction;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(name = "change_cents", nullable = false)
    private Integer changeCents;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Payment() {
    }

    public Payment(Transaction transaction, PaymentMethod paymentMethod, Integer amountCents, Integer changeCents) {
        this.id = UUID.randomUUID();
        this.transaction = transaction;
        this.paymentMethod = paymentMethod;
        this.amountCents = amountCents;
        this.changeCents = changeCents != null ? changeCents : 0;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public UUID getPaymentMethodId() {
        return paymentMethod.getId();
    }

    public String getPaymentMethodName() {
        return paymentMethod.getName();
    }

    public PaymentMethodType getPaymentMethodType() {
        return paymentMethod.getType();
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public Integer getChangeCents() {
        return changeCents;
    }

    public Integer getNetAmountCents() {
        return amountCents - changeCents;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
