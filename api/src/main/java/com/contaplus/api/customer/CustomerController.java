package com.contaplus.api.customer;

import com.contaplus.api.common.PageResponse;
import com.contaplus.api.transaction.Transaction;
import com.contaplus.api.transaction.TransactionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse criar(@Valid @RequestBody CriarCustomerRequest request) {
        CustomerService.CriarCustomerRequest serviceRequest = new CustomerService.CriarCustomerRequest(
            request.storeId(),
            request.name(),
            request.phone(),
            request.email(),
            request.cpfCnpj(),
            request.notes()
        );

        Customer customer = customerService.criar(serviceRequest);
        return toResponse(customer);
    }

    @GetMapping("/{id}")
    public CustomerResponse buscarPorId(@PathVariable UUID id) {
        Customer customer = customerService.buscarPorId(id);
        return toResponse(customer);
    }

    @GetMapping
    public PageResponse<CustomerResponse> listar(
            @RequestParam UUID storeId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return PageResponse.from(
            customerService.listarPorStorePaginado(storeId, includeInactive, search, pageable),
            this::toResponse
        );
    }

    @PutMapping("/{id}")
    public CustomerResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarCustomerRequest request) {
        CustomerService.AtualizarCustomerRequest serviceRequest = new CustomerService.AtualizarCustomerRequest(
            request.name(),
            request.phone(),
            request.email(),
            request.cpfCnpj(),
            request.notes()
        );

        Customer customer = customerService.atualizar(id, serviceRequest);
        return toResponse(customer);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        customerService.desativar(id);
    }

    @GetMapping("/{id}/purchases")
    public List<CustomerPurchaseResponse> historicoCompras(@PathVariable UUID id) {
        return customerService.buscarHistoricoCompras(id).stream()
            .map(this::toPurchaseResponse)
            .toList();
    }

    private CustomerPurchaseResponse toPurchaseResponse(Transaction transaction) {
        return new CustomerPurchaseResponse(
            transaction.getId(),
            transaction.getStatus(),
            transaction.getDescription(),
            transaction.getTotalAmountCents(),
            transaction.getOccurredAt()
        );
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getStoreId(),
            customer.getName(),
            customer.getPhone(),
            customer.getEmail(),
            customer.getCpfCnpj(),
            customer.getNotes(),
            customer.getActive(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }

    record CriarCustomerRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotBlank(message = "name is required") String name,
        String phone,
        String email,
        String cpfCnpj,
        String notes
    ) {}

    record AtualizarCustomerRequest(
        @NotBlank(message = "name is required") String name,
        String phone,
        String email,
        String cpfCnpj,
        String notes
    ) {}

    record CustomerResponse(
        UUID id,
        UUID storeId,
        String name,
        String phone,
        String email,
        String cpfCnpj,
        String notes,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}

    record CustomerPurchaseResponse(
        UUID id,
        TransactionStatus status,
        String description,
        Integer totalAmountCents,
        OffsetDateTime occurredAt
    ) {}
}
