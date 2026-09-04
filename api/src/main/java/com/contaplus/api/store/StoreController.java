package com.contaplus.api.store;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/V1/stores")
public class StoreController {
    private final StoreService service;

    StoreController(StoreService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Store criar(@RequestBody CriarStoreRequest request) {
        return service.criar(request.name());
    }

    record CriarStoreRequest(String name) {}

}
