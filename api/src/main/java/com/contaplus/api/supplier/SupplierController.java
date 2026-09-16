package com.contaplus.api.supplier;

import com.contaplus.api.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse criar(@Valid @RequestBody CriarSupplierRequest request) {
        SupplierService.CriarSupplierRequest serviceRequest = new SupplierService.CriarSupplierRequest(
            request.storeId(),
            request.name(),
            request.cnpj(),
            request.phone(),
            request.email(),
            request.address(),
            request.notes()
        );

        Supplier supplier = supplierService.criar(serviceRequest);
        return toResponse(supplier);
    }

    @GetMapping("/{id}")
    public SupplierResponse buscarPorId(@PathVariable UUID id) {
        Supplier supplier = supplierService.buscarPorId(id);
        return toResponse(supplier);
    }

    @GetMapping
    public PageResponse<SupplierResponse> listar(
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
            supplierService.listarPorStorePaginado(storeId, includeInactive, search, pageable),
            this::toResponse
        );
    }

    @PutMapping("/{id}")
    public SupplierResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarSupplierRequest request) {
        SupplierService.AtualizarSupplierRequest serviceRequest = new SupplierService.AtualizarSupplierRequest(
            request.name(),
            request.cnpj(),
            request.phone(),
            request.email(),
            request.address(),
            request.notes()
        );

        Supplier supplier = supplierService.atualizar(id, serviceRequest);
        return toResponse(supplier);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        supplierService.desativar(id);
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
            supplier.getId(),
            supplier.getStoreId(),
            supplier.getName(),
            supplier.getCnpj(),
            supplier.getPhone(),
            supplier.getEmail(),
            supplier.getAddress(),
            supplier.getNotes(),
            supplier.getActive(),
            supplier.getCreatedAt(),
            supplier.getUpdatedAt()
        );
    }

    record CriarSupplierRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotBlank(message = "name is required") String name,
        String cnpj,
        String phone,
        String email,
        String address,
        String notes
    ) {}

    record AtualizarSupplierRequest(
        @NotBlank(message = "name is required") String name,
        String cnpj,
        String phone,
        String email,
        String address,
        String notes
    ) {}

    record SupplierResponse(
        UUID id,
        UUID storeId,
        String name,
        String cnpj,
        String phone,
        String email,
        String address,
        String notes,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}
}
