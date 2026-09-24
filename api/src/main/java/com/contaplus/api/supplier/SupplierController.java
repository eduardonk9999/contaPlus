package com.contaplus.api.supplier;

import com.contaplus.api.common.PageResponse;
import com.contaplus.api.security.StoreAuthorizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/suppliers")
@Tag(name = "Fornecedores", description = "Gerenciamento de fornecedores")
public class SupplierController {

    private final SupplierService supplierService;
    private final StoreAuthorizationService storeAuth;

    SupplierController(SupplierService supplierService, StoreAuthorizationService storeAuth) {
        this.supplierService = supplierService;
        this.storeAuth = storeAuth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@storeAuthorizationService.isOwner()")
    public SupplierResponse criar(@Valid @RequestBody CriarSupplierRequest request) {
        storeAuth.validateStoreAccess(request.storeId());
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
        storeAuth.validateStoreAccess(supplier.getStoreId());
        return toResponse(supplier);
    }

    @GetMapping
    public PageResponse<SupplierResponse> listar(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        UUID authorizedStoreId = storeId != null ? storeId : storeAuth.getCurrentUserStoreId();
        storeAuth.validateStoreAccess(authorizedStoreId);

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        return PageResponse.from(
            supplierService.listarPorStorePaginado(authorizedStoreId, includeInactive, search, pageable),
            this::toResponse
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("@storeAuthorizationService.isOwner()")
    public SupplierResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarSupplierRequest request) {
        Supplier existing = supplierService.buscarPorId(id);
        storeAuth.validateStoreAccess(existing.getStoreId());

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
    @PreAuthorize("@storeAuthorizationService.isOwner()")
    public void desativar(@PathVariable UUID id) {
        Supplier existing = supplierService.buscarPorId(id);
        storeAuth.validateStoreAccess(existing.getStoreId());
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
