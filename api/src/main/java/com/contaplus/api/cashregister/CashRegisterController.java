package com.contaplus.api.cashregister;

import com.contaplus.api.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/cash-registers")
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    CashRegisterController(CashRegisterService cashRegisterService) {
        this.cashRegisterService = cashRegisterService;
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public CashRegisterResponse abrir(
            @Valid @RequestBody AbrirCaixaRequest request,
            @AuthenticationPrincipal User user
    ) {
        CashRegister cashRegister = cashRegisterService.abrir(
            request.storeId(), user, request.openingAmountCents()
        );
        return toResponse(cashRegister);
    }

    @PostMapping("/{id}/close")
    public CashRegisterResponse fechar(
            @PathVariable UUID id,
            @Valid @RequestBody FecharCaixaRequest request,
            @AuthenticationPrincipal User user
    ) {
        CashRegister cashRegister = cashRegisterService.fechar(
            id, user, request.closingAmountCents(), request.notes()
        );
        return toResponse(cashRegister);
    }

    @PostMapping("/{id}/withdrawal")
    @ResponseStatus(HttpStatus.CREATED)
    public CashMovementResponse sangria(
            @PathVariable UUID id,
            @Valid @RequestBody MovimentoRequest request,
            @AuthenticationPrincipal User user
    ) {
        CashMovement movement = cashRegisterService.registrarSangria(
            id, user, request.amountCents(), request.reason()
        );
        return toMovementResponse(movement);
    }

    @PostMapping("/{id}/supply")
    @ResponseStatus(HttpStatus.CREATED)
    public CashMovementResponse suprimento(
            @PathVariable UUID id,
            @Valid @RequestBody MovimentoRequest request,
            @AuthenticationPrincipal User user
    ) {
        CashMovement movement = cashRegisterService.registrarSuprimento(
            id, user, request.amountCents(), request.reason()
        );
        return toMovementResponse(movement);
    }

    @GetMapping("/{id}")
    public CashRegisterResponse buscarPorId(@PathVariable UUID id) {
        CashRegister cashRegister = cashRegisterService.buscarPorId(id);
        return toResponse(cashRegister);
    }

    @GetMapping("/current")
    public CashRegisterResponse buscarCaixaAberto(@RequestParam UUID storeId) {
        CashRegister cashRegister = cashRegisterService.buscarCaixaAberto(storeId);
        return toResponse(cashRegister);
    }

    @GetMapping
    public List<CashRegisterResponse> listar(@RequestParam UUID storeId) {
        return cashRegisterService.listarPorStore(storeId).stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/{id}/movements")
    public List<CashMovementResponse> listarMovimentos(@PathVariable UUID id) {
        return cashRegisterService.listarMovimentos(id).stream()
            .map(this::toMovementResponse)
            .toList();
    }

    @GetMapping("/{id}/report")
    public CashRegisterService.CashRegisterReport relatorio(@PathVariable UUID id) {
        return cashRegisterService.gerarRelatorio(id);
    }

    private CashRegisterResponse toResponse(CashRegister cr) {
        return new CashRegisterResponse(
            cr.getId(),
            cr.getStoreId(),
            cr.getOpenedById(),
            cr.getClosedById(),
            cr.getStatus(),
            cr.getOpeningAmountCents(),
            cr.getClosingAmountCents(),
            cr.getExpectedAmountCents(),
            cr.getDifferenceCents(),
            cr.getNotes(),
            cr.getOpenedAt(),
            cr.getClosedAt()
        );
    }

    private CashMovementResponse toMovementResponse(CashMovement m) {
        return new CashMovementResponse(
            m.getId(),
            m.getCashRegisterId(),
            m.getUserId(),
            m.getType(),
            m.getAmountCents(),
            m.getReason(),
            m.getOccurredAt()
        );
    }

    record AbrirCaixaRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotNull(message = "openingAmountCents is required") @PositiveOrZero Integer openingAmountCents
    ) {}

    record FecharCaixaRequest(
        @NotNull(message = "closingAmountCents is required") @PositiveOrZero Integer closingAmountCents,
        String notes
    ) {}

    record MovimentoRequest(
        @NotNull(message = "amountCents is required") @PositiveOrZero Integer amountCents,
        String reason
    ) {}

    record CashRegisterResponse(
        UUID id,
        UUID storeId,
        UUID openedById,
        UUID closedById,
        CashRegisterStatus status,
        Integer openingAmountCents,
        Integer closingAmountCents,
        Integer expectedAmountCents,
        Integer differenceCents,
        String notes,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt
    ) {}

    record CashMovementResponse(
        UUID id,
        UUID cashRegisterId,
        UUID userId,
        CashMovementType type,
        Integer amountCents,
        String reason,
        OffsetDateTime occurredAt
    ) {}
}
