package com.contaplus.api.sale;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.product.Product;
import com.contaplus.api.product.ProductRepository;
import com.contaplus.api.product.StockUnit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SalePreviewService {

    private final ProductRepository productRepository;

    SalePreviewService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public SalePreview calcularPreview(PreviewVendaRequest request) {
        List<SalePreviewItem> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        int totalAmountCents = 0;
        int totalCostCents = 0;

        for (PreviewItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.productId()));

            if (!product.getStoreId().equals(request.storeId())) {
                throw new IllegalArgumentException(
                    "Product " + itemReq.productId() + " does not belong to store " + request.storeId()
                );
            }

            if (!product.getActive()) {
                warnings.add("Product '" + product.getName() + "' is not active");
            }

            int itemTotalAmount = itemReq.quantity()
                .multiply(BigDecimal.valueOf(product.getSalePriceCents()))
                .setScale(0, RoundingMode.HALF_UP).intValue();

            int itemTotalCost = itemReq.quantity()
                .multiply(BigDecimal.valueOf(product.getCostPriceCents()))
                .setScale(0, RoundingMode.HALF_UP).intValue();

            int itemGrossProfit = itemTotalAmount - itemTotalCost;

            BigDecimal itemMargin = itemTotalAmount > 0
                ? BigDecimal.valueOf(itemGrossProfit)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(itemTotalAmount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            BigDecimal stockAfterSale = product.getStockQuantity().subtract(itemReq.quantity());

            if (stockAfterSale.compareTo(product.getMinStockQuantity()) < 0) {
                warnings.add("Product '" + product.getName() + "' will be below minimum stock level after sale");
            }

            if (stockAfterSale.compareTo(BigDecimal.ZERO) < 0) {
                warnings.add("Product '" + product.getName() + "' will have negative stock after sale: " + stockAfterSale);
            }

            items.add(new SalePreviewItem(
                product.getId(),
                product.getName(),
                product.getStockUnit(),
                itemReq.quantity(),
                product.getSalePriceCents(),
                product.getCostPriceCents(),
                itemTotalAmount,
                itemTotalCost,
                itemGrossProfit,
                itemMargin,
                product.getStockQuantity(),
                stockAfterSale
            ));

            totalAmountCents += itemTotalAmount;
            totalCostCents += itemTotalCost;
        }

        int grossProfitCents = totalAmountCents - totalCostCents;
        BigDecimal marginPercent = totalAmountCents > 0
            ? BigDecimal.valueOf(grossProfitCents)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalAmountCents), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new SalePreview(
            items,
            totalAmountCents,
            totalCostCents,
            grossProfitCents,
            marginPercent,
            warnings
        );
    }

    public record PreviewVendaRequest(
        UUID storeId,
        List<PreviewItemRequest> items
    ) {}

    public record PreviewItemRequest(
        UUID productId,
        BigDecimal quantity
    ) {}

    public record SalePreview(
        List<SalePreviewItem> items,
        Integer totalAmountCents,
        Integer totalCostCents,
        Integer grossProfitCents,
        BigDecimal marginPercent,
        List<String> warnings
    ) {}

    public record SalePreviewItem(
        UUID productId,
        String productName,
        StockUnit stockUnit,
        BigDecimal quantity,
        Integer unitPriceCents,
        Integer unitCostCents,
        Integer totalAmountCents,
        Integer totalCostCents,
        Integer grossProfitCents,
        BigDecimal marginPercent,
        BigDecimal currentStock,
        BigDecimal stockAfterSale
    ) {}
}
