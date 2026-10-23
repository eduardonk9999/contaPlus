package com.contaplus.api.expense;

import com.contaplus.api.common.PageResponse;
import com.contaplus.api.security.StoreAuthorizationService;
import com.contaplus.api.transaction.TransactionSource;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/expenses")
@Tag(name = "Despesas", description = "Contas e despesas da loja")
public class ExpenseController {

    private final ExpenseService service;
    private final StoreAuthorizationService storeAuth;

    ExpenseController(ExpenseService service, StoreAuthorizationService storeAuth) {
        this.service = service;
        this.storeAuth = storeAuth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@storeAuthorizationService.isOwner()")
    public ExpenseResponse registrar(@Valid @RequestBody RegistrarDespesaRequest request) {
        storeAuth.validateStoreAccess(request.storeId());
        Expense expense = service.registrar(new ExpenseService.RegistrarDespesa(
            request.storeId(),
            request.idempotencyKey(),
            request.description(),
            request.amountCents(),
            request.category(),
            request.occurredAt(),
            request.source(),
            request.originalInput()
        ));
        return toResponse(expense);
    }

    @GetMapping
    public PageResponse<ExpenseResponse> listar(
            @RequestParam(required = false) UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID authorizedStoreId = storeId != null ? storeId : storeAuth.getCurrentUserStoreId();
        storeAuth.validateStoreAccess(authorizedStoreId);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());
        return PageResponse.from(service.listar(authorizedStoreId, startDate, endDate, pageable), this::toResponse);
    }

    private ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
            expense.getId(),
            expense.getStoreId(),
            expense.getDescription(),
            expense.getAmountCents(),
            expense.getCategory(),
            expense.getOccurredAt(),
            expense.getSource(),
            expense.getOriginalInput()
        );
    }

    record RegistrarDespesaRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotNull(message = "idempotencyKey is required") UUID idempotencyKey,
        @NotBlank(message = "description is required") String description,
        @NotNull(message = "amountCents is required") @Min(1) Integer amountCents,
        ExpenseCategory category,
        OffsetDateTime occurredAt,
        TransactionSource source,
        String originalInput
    ) {}

    record ExpenseResponse(
        UUID id,
        UUID storeId,
        String description,
        Integer amountCents,
        ExpenseCategory category,
        OffsetDateTime occurredAt,
        TransactionSource source,
        String originalInput
    ) {}
}
