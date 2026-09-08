package com.contaplus.api.store;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    public StoreResponse criar(@RequestBody CriarStoreRequest request) {
        Store store = service.criar(request.name());
        return new StoreResponse(store.getId(), store.getName());
    }

    // DTO de entrada
    record CriarStoreRequest(String name) {}

    // DTO de saida
    record StoreResponse(UUID id, String name) {}

    record StoreResponse(UUID id, String name) {}
}
