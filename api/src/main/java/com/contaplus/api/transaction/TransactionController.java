package com.contaplus.api.transaction;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.product.StockUnit;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {

    private final TransactionRepository repository;

    TransactionController(TransactionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public TransactionResponse buscarPorId(@PathVariable UUID id) {
        Transaction transaction = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        return toResponse(transaction);
    }

    @GetMapping
    public List<TransactionResponse> listarPorStore(@RequestParam UUID storeId) {
        return repository.findByStore_IdOrderByOccurredAtDesc(storeId).stream()
            .map(this::toResponse)
            .toList();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        List<TransactionItemResponse> items = transaction.getItems().stream()
            .map(this::toItemResponse)
            .toList();

        return new TransactionResponse(
            transaction.getId(),
            transaction.getStoreId(),
            transaction.getType(),
            transaction.getStatus(),
            transaction.getSource(),
            transaction.getDescription(),
            transaction.getTotalAmountCents(),
            transaction.getTotalCostCents(),
            transaction.getGrossProfitCents(),
            transaction.getMarginPercent(),
            items,
            transaction.getOccurredAt(),
            transaction.getIdempotencyKey()
        );
    }

    private TransactionItemResponse toItemResponse(TransactionItem item) {
        return new TransactionItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductNameSnapshot(),
            item.getStockUnitSnapshot(),
            item.getQuantity(),
            item.getUnitPriceCents(),
            item.getUnitCostCents(),
            item.getTotalAmountCents(),
            item.getTotalCostCents(),
            item.getGrossProfitCents(),
            item.getMarginPercent()
        );
    }

    record TransactionResponse(
        UUID id,
        UUID storeId,
        TransactionType type,
        TransactionStatus status,
        TransactionSource source,
        String description,
        Integer totalAmountCents,
        Integer totalCostCents,
        Integer grossProfitCents,
        BigDecimal marginPercent,
        List<TransactionItemResponse> items,
        OffsetDateTime occurredAt,
        UUID idempotencyKey
    ) {}

    record TransactionItemResponse(
        UUID id,
        UUID productId,
        String productName,
        StockUnit stockUnit,
        BigDecimal quantity,
        Integer unitPriceCents,
        Integer unitCostCents,
        Integer totalAmountCents,
        Integer totalCostCents,
        Integer grossProfitCents,
        BigDecimal marginPercent
    ) {}
}
