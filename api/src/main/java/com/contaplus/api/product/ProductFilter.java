package com.contaplus.api.product;

import com.contaplus.api.common.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFilter(
    UUID storeId,
    String search,
    UUID categoryId,
    ProductType type,
    Boolean active,
    Integer minPrice,
    Integer maxPrice,
    Boolean lowStock
) {
    public Specification<Product> toSpecification() {
        SpecificationBuilder<Product> builder = new SpecificationBuilder<>();

        builder.equalNested("store", "id", storeId);
        builder.like("name", search);
        builder.equal("categoryId", categoryId);
        builder.equal("type", type);
        builder.equal("active", active);
        builder.betweenInt("salePriceCents", minPrice, maxPrice);

        if (Boolean.TRUE.equals(lowStock)) {
            builder.equal("active", true);
            // Low stock: stockQuantity <= minStockQuantity
            return builder.build().and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("stockQuantity"), root.get("minStockQuantity"))
            );
        }

        return builder.build();
    }
}
