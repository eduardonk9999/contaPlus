package com.contaplus.api.purchase;

import com.contaplus.api.common.PageResponse;
import com.contaplus.api.product.StockUnit;
import com.contaplus.api.security.StoreAuthorizationService;
import com.contaplus.api.transaction.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/purchases")
@Tag(name = "Compras", description = "Registro de compras/entradas")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final StoreAuthorizationService storeAuth;

    PurchaseController(PurchaseService purchaseService, StoreAuthorizationService storeAuth) {
        this.purchaseService = purchaseService;
        this.storeAuth = storeAuth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@storeAuthorizationService.isOwner()")
    public PurchaseResponse registrar(@Valid @RequestBody RegistrarCompraRequest request) {
        storeAuth.validateStoreAccess(request.storeId());
        PurchaseService.RegistrarCompraRequest serviceRequest = new PurchaseService.RegistrarCompraRequest(
            request.storeId(),
            request.idempotencyKey(),
            request.description(),
            request.items().stream()
                .map(i -> new PurchaseService.ItemCompraRequest(i.productId(), i.quantity(), i.unitCostCents()))
                .toList(),
            request.source(),
            request.occurredAt(),
            request.supplierId()
        );

        Transaction transaction = purchaseService.registrarCompra(serviceRequest);
        return toResponse(transaction);
    }

    @GetMapping("/{id}")
    public PurchaseResponse buscarPorId(@PathVariable UUID id) {
        Transaction transaction = purchaseService.buscarPorId(id);
        storeAuth.validateStoreAccess(transaction.getStoreId());
        return toResponse(transaction);
    }

    @GetMapping
    public PageResponse<PurchaseResponse> listarPorStore(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(required = false) Integer minAmount,
            @RequestParam(required = false) Integer maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID authorizedStoreId = storeId != null ? storeId : storeAuth.getCurrentUserStoreId();
        storeAuth.validateStoreAccess(authorizedStoreId);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());

        TransactionFilter filter = new TransactionFilter(
            authorizedStoreId, TransactionType.PURCHASE, null,
            null, supplierId,
            startDate, endDate,
            minAmount, maxAmount
        );

        return PageResponse.from(
            purchaseService.listarComFiltros(filter, pageable),
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
            transaction.getSupplierId(),
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
        OffsetDateTime occurredAt,
        UUID supplierId
    ) {}

    record ItemRequest(
        @NotNull(message = "productId is required") UUID productId,
        @NotNull(message = "quantity is required") @Positive BigDecimal quantity,
        @Positive Integer unitCostCents
    ) {}

    record PurchaseResponse(
        UUID id,
        UUID storeId,
        UUID supplierId,
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
