package com.contaplus.api.store;

import com.contaplus.api.security.StoreAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stores")
@Tag(name = "Lojas", description = "Gerenciamento da loja do usuário")
public class StoreController {
    private final StoreService service;
    private final StoreAuthorizationService storeAuth;

    StoreController(StoreService service, StoreAuthorizationService storeAuth) {
        this.service = service;
        this.storeAuth = storeAuth;
    }

    @GetMapping("/me")
    @Operation(summary = "Buscar minha loja", description = "Retorna a loja do usuário autenticado")
    public StoreResponse minhaLoja() {
        UUID storeId = storeAuth.getCurrentUserStoreId();
        Store store = service.buscarPorId(storeId);
        return toResponse(store);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar loja por ID")
    public StoreResponse buscarPorId(@PathVariable UUID id) {
        storeAuth.validateStoreAccess(id);
        Store store = service.buscarPorId(id);
        return toResponse(store);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@storeAuthorizationService.isOwner()")
    @Operation(summary = "Atualizar loja", description = "Apenas o proprietário pode atualizar")
    public StoreResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarStoreRequest request) {
        storeAuth.validateStoreAccess(id);
        Store store = service.atualizar(id, request.name());
        return toResponse(store);
    }

    private StoreResponse toResponse(Store store) {
        return new StoreResponse(store.getId(), store.getName());
    }

    record CriarStoreRequest(@NotBlank(message = "name is required") String name) {}

    record AtualizarStoreRequest(@NotBlank(message = "name is required") String name) {}

    record StoreResponse(UUID id, String name) {}
}
