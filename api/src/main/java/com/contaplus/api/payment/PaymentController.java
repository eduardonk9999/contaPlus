package com.contaplus.api.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Pagamentos", description = "Registro de pagamentos em transações")
public class PaymentController {

    private final PaymentService service;

    PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/transaction/{transactionId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar pagamentos em uma transação",
               description = "Registra um ou mais pagamentos. O total deve cobrir o valor da transação.")
    public List<PaymentResponse> registrarPagamentos(
            @PathVariable UUID transactionId,
            @Valid @RequestBody RegistrarPagamentosRequest request
    ) {
        List<PaymentService.PagamentoRequest> pagamentos = request.payments().stream()
            .map(p -> new PaymentService.PagamentoRequest(p.paymentMethodId(), p.amountCents(), p.changeCents()))
            .toList();

        return service.registrarPagamentos(transactionId, pagamentos).stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Listar pagamentos de uma transação")
    public List<PaymentResponse> listarPorTransacao(@PathVariable UUID transactionId) {
        return service.listarPorTransacao(transactionId).stream()
            .map(this::toResponse)
            .toList();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getPaymentMethodId(),
            payment.getPaymentMethodName(),
            payment.getPaymentMethodType(),
            payment.getAmountCents(),
            payment.getChangeCents(),
            payment.getNetAmountCents(),
            payment.getCreatedAt().toString()
        );
    }

    record RegistrarPagamentosRequest(
        @NotEmpty(message = "At least one payment is required")
        List<PagamentoItemRequest> payments
    ) {}

    record PagamentoItemRequest(
        @NotNull(message = "paymentMethodId is required") UUID paymentMethodId,
        @NotNull(message = "amountCents is required") @Min(1) Integer amountCents,
        @Min(0) Integer changeCents
    ) {}

    record PaymentResponse(
        UUID id,
        UUID paymentMethodId,
        String paymentMethodName,
        PaymentMethodType paymentMethodType,
        Integer amountCents,
        Integer changeCents,
        Integer netAmountCents,
        String createdAt
    ) {}
}
