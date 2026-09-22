package com.contaplus.api.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payment-methods")
@Tag(name = "Formas de Pagamento", description = "Gerenciamento de formas de pagamento")
public class PaymentMethodController {

    private final PaymentMethodService service;

    PaymentMethodController(PaymentMethodService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar forma de pagamento")
    public PaymentMethodResponse criar(@Valid @RequestBody CriarPaymentMethodRequest request) {
        PaymentMethod pm = service.criar(
            request.storeId(),
            request.name(),
            request.type(),
            request.acceptsChange() != null && request.acceptsChange()
        );
        return toResponse(pm);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar forma de pagamento por ID")
    public PaymentMethodResponse buscarPorId(@PathVariable UUID id) {
        return toResponse(service.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar formas de pagamento da loja")
    public List<PaymentMethodResponse> listarPorStore(
            @RequestParam UUID storeId,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return service.listarPorStore(storeId, includeInactive).stream()
            .map(this::toResponse)
            .toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar forma de pagamento")
    public PaymentMethodResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarPaymentMethodRequest request
    ) {
        PaymentMethod pm = service.atualizar(
            id,
            request.name(),
            request.type(),
            request.acceptsChange() != null && request.acceptsChange()
        );
        return toResponse(pm);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativar forma de pagamento")
    public void desativar(@PathVariable UUID id) {
        service.desativar(id);
    }

    private PaymentMethodResponse toResponse(PaymentMethod pm) {
        return new PaymentMethodResponse(
            pm.getId(),
            pm.getStoreId(),
            pm.getName(),
            pm.getType(),
            pm.getAcceptsChange(),
            pm.getActive()
        );
    }

    record CriarPaymentMethodRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotBlank(message = "name is required") String name,
        @NotNull(message = "type is required") PaymentMethodType type,
        Boolean acceptsChange
    ) {}

    record AtualizarPaymentMethodRequest(
        @NotBlank(message = "name is required") String name,
        @NotNull(message = "type is required") PaymentMethodType type,
        Boolean acceptsChange
    ) {}

    record PaymentMethodResponse(
        UUID id,
        UUID storeId,
        String name,
        PaymentMethodType type,
        Boolean acceptsChange,
        Boolean active
    ) {}
}
