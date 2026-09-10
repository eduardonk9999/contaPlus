package com.contaplus.api.store;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stores")
public class StoreController {
    private final StoreService service;

    StoreController(StoreService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse criar(@Valid @RequestBody CriarStoreRequest request) {
        Store store = service.criar(request.name());
        return toResponse(store);
    }

    @GetMapping("/{id}")
    public StoreResponse buscarPorId(@PathVariable UUID id) {
        Store store = service.buscarPorId(id);
        return toResponse(store);
    }

    @GetMapping
    public List<StoreResponse> listarTodas() {
        return service.listarTodas().stream()
            .map(this::toResponse)
            .toList();
    }

    @PutMapping("/{id}")
    public StoreResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarStoreRequest request) {
        Store store = service.atualizar(id, request.name());
        return toResponse(store);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        service.deletar(id);
    }

    private StoreResponse toResponse(Store store) {
        return new StoreResponse(store.getId(), store.getName());
    }

    record CriarStoreRequest(@NotBlank(message = "name is required") String name) {}

    record AtualizarStoreRequest(@NotBlank(message = "name is required") String name) {}

    record StoreResponse(UUID id, String name) {}
}
