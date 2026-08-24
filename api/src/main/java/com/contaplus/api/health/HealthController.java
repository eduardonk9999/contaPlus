package com.contaplus.api.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/v1/health")
    HealthResponse health() {
        return new HealthResponse("ok", "Conta+ API");
    }


    record HealthResponse(String status, String service){}
}
