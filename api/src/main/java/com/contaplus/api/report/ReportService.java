package com.contaplus.api.report;

import com.contaplus.api.product.Product;
import com.contaplus.api.product.ProductRepository;
import com.contaplus.api.stock.StockMovement;
import com.contaplus.api.stock.StockMovementRepository;
import com.contaplus.api.transaction.Transaction;
import com.contaplus.api.transaction.TransactionItem;
import com.contaplus.api.transaction.TransactionRepository;
import com.contaplus.api.transaction.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    ReportService(TransactionRepository transactionRepository,
                  ProductRepository productRepository,
                  StockMovementRepository stockMovementRepository) {
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public SalesReport vendasPorPeriodo(UUID storeId, OffsetDateTime startDate, OffsetDateTime endDate) {
        List<Transaction> transactions = transactionRepository
            .findByStore_IdAndOccurredAtBetweenOrderByOccurredAtDesc(storeId, startDate, endDate)
            .stream()
            .filter(t -> t.getType() == TransactionType.SALE)
            .toList();

        int totalSales = transactions.size();
        int totalAmountCents = 0;
        int totalCostCents = 0;
        int cancelledCount = 0;

        for (Transaction t : transactions) {
            if (t.getStatus() == com.contaplus.api.transaction.TransactionStatus.CANCELLED) {
                cancelledCount++;
            } else {
                totalAmountCents += t.getTotalAmountCents();
                totalCostCents += t.getTotalCostCents();
            }
        }

        int grossProfitCents = totalAmountCents - totalCostCents;
        BigDecimal marginPercent = totalAmountCents > 0
            ? BigDecimal.valueOf(grossProfitCents * 100).divide(BigDecimal.valueOf(totalAmountCents), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        int averageTicketCents = (totalSales - cancelledCount) > 0
            ? totalAmountCents / (totalSales - cancelledCount)
            : 0;

        return new SalesReport(
            startDate,
            endDate,
            totalSales,
            totalSales - cancelledCount,
            cancelledCount,
            totalAmountCents,
            totalCostCents,
            grossProfitCents,
            marginPercent,
            averageTicketCents
        );
    }

    public List<TopSellingProduct> produtosMaisVendidos(UUID storeId, OffsetDateTime startDate, OffsetDateTime endDate, int limit) {
        List<Transaction> transactions = transactionRepository
            .findByStore_IdAndOccurredAtBetweenOrderByOccurredAtDesc(storeId, startDate, endDate)
            .stream()
            .filter(t -> t.getType() == TransactionType.SALE)
            .filter(t -> t.getStatus() != com.contaplus.api.transaction.TransactionStatus.CANCELLED)
            .toList();

        Map<UUID, ProductSalesAccumulator> productSales = new HashMap<>();

        for (Transaction t : transactions) {
            for (TransactionItem item : t.getItems()) {
                UUID productId = item.getProductId();
                productSales.computeIfAbsent(productId, k -> new ProductSalesAccumulator(
                    productId,
                    item.getProductNameSnapshot()
                ));
                productSales.get(productId).add(item);
            }
        }

        return productSales.values().stream()
            .sorted((a, b) -> Integer.compare(b.totalAmountCents, a.totalAmountCents))
            .limit(limit)
            .map(acc -> new TopSellingProduct(
                acc.productId,
                acc.productName,
                acc.quantitySold,
                acc.totalAmountCents,
                acc.totalCostCents,
                acc.totalAmountCents - acc.totalCostCents,
                acc.salesCount
            ))
            .toList();
    }

    public List<ProductMarginReport> margemPorProduto(UUID storeId) {
        List<Product> products = productRepository.findByStore_IdAndActiveTrue(storeId);

        return products.stream()
            .map(p -> {
                int marginCents = p.getSalePriceCents() - p.getCostPriceCents();
                BigDecimal marginPercent = p.getSalePriceCents() > 0
                    ? BigDecimal.valueOf(marginCents * 100).divide(BigDecimal.valueOf(p.getSalePriceCents()), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                return new ProductMarginReport(
                    p.getId(),
                    p.getName(),
                    p.getCostPriceCents(),
                    p.getSalePriceCents(),
                    marginCents,
                    marginPercent
                );
            })
            .sorted((a, b) -> b.marginPercent().compareTo(a.marginPercent()))
            .toList();
    }

    public List<LowStockProduct> produtosEstoqueBaixo(UUID storeId) {
        List<Product> products = productRepository.findByStore_IdAndActiveTrue(storeId);

        return products.stream()
            .filter(Product::isLowStock)
            .map(p -> new LowStockProduct(
                p.getId(),
                p.getName(),
                p.getStockQuantity(),
                p.getMinStockQuantity(),
                p.getStockUnit().name()
            ))
            .sorted((a, b) -> a.stockQuantity().compareTo(b.stockQuantity()))
            .toList();
    }

    public List<StockMovementReport> movimentacoesEstoque(UUID storeId, OffsetDateTime startDate, OffsetDateTime endDate) {
        List<StockMovement> movements = stockMovementRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
            .stream()
            .filter(m -> !m.getOccurredAt().isBefore(startDate) && !m.getOccurredAt().isAfter(endDate))
            .toList();

        return movements.stream()
            .map(m -> new StockMovementReport(
                m.getId(),
                m.getProductId(),
                m.getProduct().getName(),
                m.getType().name(),
                m.getQuantityDelta(),
                m.getQuantityBefore(),
                m.getQuantityAfter(),
                m.getReason(),
                m.getOccurredAt()
            ))
            .toList();
    }

    public DashboardReport dashboard(UUID storeId, OffsetDateTime startDate, OffsetDateTime endDate) {
        SalesReport sales = vendasPorPeriodo(storeId, startDate, endDate);
        List<TopSellingProduct> topProducts = produtosMaisVendidos(storeId, startDate, endDate, 5);
        List<LowStockProduct> lowStock = produtosEstoqueBaixo(storeId);

        return new DashboardReport(sales, topProducts, lowStock.size(), lowStock.stream().limit(5).toList());
    }

    private static class ProductSalesAccumulator {
        UUID productId;
        String productName;
        BigDecimal quantitySold = BigDecimal.ZERO;
        int totalAmountCents = 0;
        int totalCostCents = 0;
        int salesCount = 0;

        ProductSalesAccumulator(UUID productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }

        void add(TransactionItem item) {
            this.quantitySold = this.quantitySold.add(item.getQuantity());
            this.totalAmountCents += item.getTotalAmountCents();
            this.totalCostCents += item.getTotalCostCents();
            this.salesCount++;
        }
    }

    public record SalesReport(
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        int totalTransactions,
        int confirmedTransactions,
        int cancelledTransactions,
        int totalAmountCents,
        int totalCostCents,
        int grossProfitCents,
        BigDecimal marginPercent,
        int averageTicketCents
    ) {}

    public record TopSellingProduct(
        UUID productId,
        String productName,
        BigDecimal quantitySold,
        int totalAmountCents,
        int totalCostCents,
        int grossProfitCents,
        int salesCount
    ) {}

    public record ProductMarginReport(
        UUID productId,
        String productName,
        int costPriceCents,
        int salePriceCents,
        int marginCents,
        BigDecimal marginPercent
    ) {}

    public record LowStockProduct(
        UUID productId,
        String productName,
        BigDecimal stockQuantity,
        BigDecimal minStockQuantity,
        String stockUnit
    ) {}

    public record StockMovementReport(
        UUID id,
        UUID productId,
        String productName,
        String type,
        BigDecimal quantityDelta,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        String reason,
        OffsetDateTime occurredAt
    ) {}

    public record DashboardReport(
        SalesReport salesSummary,
        List<TopSellingProduct> topProducts,
        int lowStockCount,
        List<LowStockProduct> lowStockProducts
    ) {}
}
