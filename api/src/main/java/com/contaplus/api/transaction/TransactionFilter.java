package com.contaplus.api.transaction;

import com.contaplus.api.common.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionFilter(
    UUID storeId,
    TransactionType type,
    TransactionStatus status,
    UUID customerId,
    UUID supplierId,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    Integer minAmount,
    Integer maxAmount
) {
    public Specification<Transaction> toSpecification() {
        SpecificationBuilder<Transaction> builder = new SpecificationBuilder<>();

        builder.equalNested("store", "id", storeId);
        builder.equal("type", type);
        builder.equal("status", status);
        builder.equalNested("customer", "id", customerId);
        builder.equalNested("supplier", "id", supplierId);
        builder.between("occurredAt", startDate, endDate);
        builder.betweenInt("totalAmountCents", minAmount, maxAmount);

        return builder.build();
    }
}
