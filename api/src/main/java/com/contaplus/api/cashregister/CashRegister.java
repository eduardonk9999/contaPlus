package com.contaplus.api.cashregister;

import com.contaplus.api.store.Store;
import com.contaplus.api.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cash_registers")
public class CashRegister {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by", nullable = false)
    private User openedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CashRegisterStatus status;

    @Column(name = "opening_amount_cents", nullable = false)
    private Integer openingAmountCents;

    @Column(name = "closing_amount_cents")
    private Integer closingAmountCents;

    @Column(name = "expected_amount_cents")
    private Integer expectedAmountCents;

    @Column(name = "difference_cents")
    private Integer differenceCents;

    @Column
    private String notes;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "cashRegister", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CashMovement> movements = new ArrayList<>();

    protected CashRegister() {
    }

    public CashRegister(Store store, User openedBy, Integer openingAmountCents) {
        this.id = UUID.randomUUID();
        this.store = store;
        this.openedBy = openedBy;
        this.status = CashRegisterStatus.OPEN;
        this.openingAmountCents = openingAmountCents;
        this.openedAt = OffsetDateTime.now();
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

    public User getOpenedBy() {
        return openedBy;
    }

    public UUID getOpenedById() {
        return openedBy.getId();
    }

    public User getClosedBy() {
        return closedBy;
    }

    public UUID getClosedById() {
        return closedBy != null ? closedBy.getId() : null;
    }

    public CashRegisterStatus getStatus() {
        return status;
    }

    public Integer getOpeningAmountCents() {
        return openingAmountCents;
    }

    public Integer getClosingAmountCents() {
        return closingAmountCents;
    }

    public Integer getExpectedAmountCents() {
        return expectedAmountCents;
    }

    public Integer getDifferenceCents() {
        return differenceCents;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<CashMovement> getMovements() {
        return movements;
    }

    public boolean isOpen() {
        return status == CashRegisterStatus.OPEN;
    }

    public void close(User closedBy, Integer closingAmountCents, Integer expectedAmountCents, String notes) {
        this.closedBy = closedBy;
        this.status = CashRegisterStatus.CLOSED;
        this.closingAmountCents = closingAmountCents;
        this.expectedAmountCents = expectedAmountCents;
        this.differenceCents = closingAmountCents - expectedAmountCents;
        this.notes = notes;
        this.closedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void addMovement(CashMovement movement) {
        movements.add(movement);
        this.updatedAt = OffsetDateTime.now();
    }

    public int calculateExpectedAmount() {
        int total = openingAmountCents;
        for (CashMovement movement : movements) {
            switch (movement.getType()) {
                case SUPPLY, SALE -> total += movement.getAmountCents();
                case WITHDRAWAL, REFUND -> total -= movement.getAmountCents();
                default -> {}
            }
        }
        return total;
    }
}
