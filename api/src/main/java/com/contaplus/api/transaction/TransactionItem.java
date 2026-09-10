package com.contaplus.api.transaction;

import com.contaplus.api.product.Product;
import com.contaplus.api.product.StockUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_items")
public class TransactionItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_unit_snapshot", nullable = false)
    private StockUnit stockUnitSnapshot;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private Integer unitPriceCents;

    @Column(name = "unit_cost_cents", nullable = false)
    private Integer unitCostCents;

    @Column(name = "total_amount_cents", nullable = false)
    private Integer totalAmountCents;

    @Column(name = "total_cost_cents", nullable = false)
    private Integer totalCostCents;

    @Column(name = "gross_profit_cents", nullable = false)
    private Integer grossProfitCents;

    @Column(name = "margin_percent", precision = 5, scale = 2)
    private BigDecimal marginPercent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TransactionItem() {
    }

    public TransactionItem(Transaction transaction, Product product, BigDecimal quantity) {
        this.id = UUID.randomUUID();
        this.transaction = transaction;
        this.product = product;
        this.productNameSnapshot = product.getName();
        this.stockUnitSnapshot = product.getStockUnit();
        this.quantity = quantity;
        this.unitPriceCents = product.getSalePriceCents();
        this.unitCostCents = product.getCostPriceCents();

        this.totalAmountCents = quantity.multiply(BigDecimal.valueOf(unitPriceCents))
            .setScale(0, RoundingMode.HALF_UP).intValue();
        this.totalCostCents = quantity.multiply(BigDecimal.valueOf(unitCostCents))
            .setScale(0, RoundingMode.HALF_UP).intValue();
        this.grossProfitCents = this.totalAmountCents - this.totalCostCents;

        if (this.totalAmountCents > 0) {
            this.marginPercent = BigDecimal.valueOf(this.grossProfitCents)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(this.totalAmountCents), 2, RoundingMode.HALF_UP);
        } else {
            this.marginPercent = BigDecimal.ZERO;
        }

        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Product getProduct() {
        return product;
    }

    public UUID getProductId() {
        return product.getId();
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public StockUnit getStockUnitSnapshot() {
        return stockUnitSnapshot;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Integer getUnitPriceCents() {
        return unitPriceCents;
    }

    public Integer getUnitCostCents() {
        return unitCostCents;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
