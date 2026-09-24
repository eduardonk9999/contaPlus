package com.contaplus.api.sale;

import com.contaplus.api.common.PageResponse;
import com.contaplus.api.product.StockUnit;
import com.contaplus.api.security.StoreAuthorizationService;
import com.contaplus.api.transaction.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/v1/sales")
@Tag(name = "Vendas", description = "Registro e gerenciamento de vendas")
public class SaleController {

    private final SaleService saleService;
    private final SalePreviewService previewService;
    private final StoreAuthorizationService storeAuth;

    SaleController(SaleService saleService, SalePreviewService previewService, StoreAuthorizationService storeAuth) {
        this.saleService = saleService;
        this.previewService = previewService;
        this.storeAuth = storeAuth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse confirmar(@Valid @RequestBody ConfirmarVendaRequest request) {
        storeAuth.validateStoreAccess(request.storeId());
        SaleService.ConfirmarVendaRequest serviceRequest = new SaleService.ConfirmarVendaRequest(
            request.storeId(),
            request.idempotencyKey(),
            request.description(),
            request.items().stream()
                .map(i -> new SaleService.ItemVendaRequest(i.productId(), i.quantity()))
                .toList(),
            request.source(),
            request.originalInput(),
            request.occurredAt(),
            request.customerId()
        );

        Transaction transaction = saleService.confirmarVenda(serviceRequest);
        return toResponse(transaction);
    }

    @PostMapping("/preview")
    public SalePreviewService.SalePreview preview(@Valid @RequestBody PreviewRequest request) {
        storeAuth.validateStoreAccess(request.storeId());
        SalePreviewService.PreviewVendaRequest serviceRequest = new SalePreviewService.PreviewVendaRequest(
            request.storeId(),
            request.items().stream()
                .map(i -> new SalePreviewService.PreviewItemRequest(i.productId(), i.quantity()))
                .toList()
        );

        return previewService.calcularPreview(serviceRequest);
    }

    @GetMapping("/{id}")
    public SaleResponse buscarPorId(@PathVariable UUID id) {
        Transaction transaction = saleService.buscarPorId(id);
        storeAuth.validateStoreAccess(transaction.getStoreId());
        return toResponse(transaction);
    }

    @PutMapping("/{id}/cancel")
    public SaleResponse cancelar(@PathVariable UUID id) {
        Transaction transaction = saleService.buscarPorId(id);
        storeAuth.validateStoreAccess(transaction.getStoreId());
        transaction = saleService.cancelarVenda(id);
        return toResponse(transaction);
    }

    @GetMapping
    public PageResponse<SaleResponse> listarPorStore(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) UUID customerId,
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
            authorizedStoreId, TransactionType.SALE, status,
            customerId, null,
            startDate, endDate,
            minAmount, maxAmount
        );

        return PageResponse.from(
            saleService.listarComFiltros(filter, pageable),
            this::toResponse
        );
    }

    private SaleResponse toResponse(Transaction transaction) {
        List<SaleItemResponse> items = transaction.getItems().stream()
            .map(this::toItemResponse)
            .toList();

        return new SaleResponse(
            transaction.getId(),
            transaction.getStoreId(),
            transaction.getCustomerId(),
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

    private SaleItemResponse toItemResponse(TransactionItem item) {
        return new SaleItemResponse(
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

    record ConfirmarVendaRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotNull(message = "idempotencyKey is required") UUID idempotencyKey,
        String description,
        @NotEmpty(message = "items cannot be empty") List<ItemRequest> items,
        TransactionSource source,
        String originalInput,
        OffsetDateTime occurredAt,
        UUID customerId
    ) {}

    record PreviewRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotEmpty(message = "items cannot be empty") List<ItemRequest> items
    ) {}

    record ItemRequest(
        @NotNull(message = "productId is required") UUID productId,
        @NotNull(message = "quantity is required") @Positive BigDecimal quantity
    ) {}

    record SaleResponse(
        UUID id,
        UUID storeId,
        UUID customerId,
        TransactionStatus status,
        TransactionSource source,
        String description,
        Integer totalAmountCents,
        Integer totalCostCents,
        Integer grossProfitCents,
        BigDecimal marginPercent,
        List<SaleItemResponse> items,
        OffsetDateTime occurredAt,
        UUID idempotencyKey
    ) {}

    record SaleItemResponse(
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
