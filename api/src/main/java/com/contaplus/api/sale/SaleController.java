package com.contaplus.api.sale;

import com.contaplus.api.product.StockUnit;
import com.contaplus.api.transaction.Transaction;
import com.contaplus.api.transaction.TransactionItem;
import com.contaplus.api.transaction.TransactionSource;
import com.contaplus.api.transaction.TransactionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/sales")
public class SaleController {

    private final SaleService saleService;
    private final SalePreviewService previewService;

    SaleController(SaleService saleService, SalePreviewService previewService) {
        this.saleService = saleService;
        this.previewService = previewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse confirmar(@Valid @RequestBody ConfirmarVendaRequest request) {
        SaleService.ConfirmarVendaRequest serviceRequest = new SaleService.ConfirmarVendaRequest(
            request.storeId(),
            request.idempotencyKey(),
            request.description(),
            request.items().stream()
                .map(i -> new SaleService.ItemVendaRequest(i.productId(), i.quantity()))
                .toList(),
            request.source(),
            request.originalInput(),
            request.occurredAt()
        );

        Transaction transaction = saleService.confirmarVenda(serviceRequest);
        return toResponse(transaction);
    }

    @PostMapping("/preview")
    public SalePreviewService.SalePreview preview(@Valid @RequestBody PreviewRequest request) {
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
        return toResponse(transaction);
    }

    @GetMapping
    public List<SaleResponse> listarPorStore(@RequestParam UUID storeId) {
        return saleService.listarVendasPorStore(storeId).stream()
            .map(this::toResponse)
            .toList();
    }

    private SaleResponse toResponse(Transaction transaction) {
        List<SaleItemResponse> items = transaction.getItems().stream()
            .map(this::toItemResponse)
            .toList();

        return new SaleResponse(
            transaction.getId(),
            transaction.getStoreId(),
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
        OffsetDateTime occurredAt
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
