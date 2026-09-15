package com.contaplus.api.purchase;

import com.contaplus.api.common.PageResponse;
import com.contaplus.api.product.StockUnit;
import com.contaplus.api.transaction.Transaction;
import com.contaplus.api.transaction.TransactionItem;
import com.contaplus.api.transaction.TransactionSource;
import com.contaplus.api.transaction.TransactionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse registrar(@Valid @RequestBody RegistrarCompraRequest request) {
        PurchaseService.RegistrarCompraRequest serviceRequest = new PurchaseService.RegistrarCompraRequest(
            request.storeId(),
            request.idempotencyKey(),
            request.description(),
            request.items().stream()
                .map(i -> new PurchaseService.ItemCompraRequest(i.productId(), i.quantity(), i.unitCostCents()))
                .toList(),
            request.source(),
            request.occurredAt()
        );

        Transaction transaction = purchaseService.registrarCompra(serviceRequest);
        return toResponse(transaction);
    }

    @GetMapping("/{id}")
    public PurchaseResponse buscarPorId(@PathVariable UUID id) {
        Transaction transaction = purchaseService.buscarPorId(id);
        return toResponse(transaction);
    }

    @GetMapping
    public PageResponse<PurchaseResponse> listarPorStore(
            @RequestParam UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());
        return PageResponse.from(
            purchaseService.listarComprasPorStorePaginado(storeId, pageable),
            this::toResponse
        );
    }

    private PurchaseResponse toResponse(Transaction transaction) {
        List<PurchaseItemResponse> items = transaction.getItems().stream()
            .map(this::toItemResponse)
            .toList();

        return new PurchaseResponse(
            transaction.getId(),
            transaction.getStoreId(),
            transaction.getStatus(),
            transaction.getSource(),
            transaction.getDescription(),
            transaction.getTotalCostCents(),
            items,
            transaction.getOccurredAt(),
            transaction.getIdempotencyKey()
        );
    }

    private PurchaseItemResponse toItemResponse(TransactionItem item) {
        return new PurchaseItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductNameSnapshot(),
            item.getStockUnitSnapshot(),
            item.getQuantity(),
            item.getUnitCostCents(),
            item.getTotalCostCents()
        );
    }

    record RegistrarCompraRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotNull(message = "idempotencyKey is required") UUID idempotencyKey,
        String description,
        @NotEmpty(message = "items cannot be empty") List<ItemRequest> items,
        TransactionSource source,
        OffsetDateTime occurredAt
    ) {}

    record ItemRequest(
        @NotNull(message = "productId is required") UUID productId,
        @NotNull(message = "quantity is required") @Positive BigDecimal quantity,
        @Positive Integer unitCostCents
    ) {}

    record PurchaseResponse(
        UUID id,
        UUID storeId,
        TransactionStatus status,
        TransactionSource source,
        String description,
        Integer totalCostCents,
        List<PurchaseItemResponse> items,
        OffsetDateTime occurredAt,
        UUID idempotencyKey
    ) {}

    record PurchaseItemResponse(
        UUID id,
        UUID productId,
        String productName,
        StockUnit stockUnit,
        BigDecimal quantity,
        Integer unitCostCents,
        Integer totalCostCents
    ) {}
}
