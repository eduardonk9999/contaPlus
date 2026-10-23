package com.contaplus.api.voice;

import com.contaplus.api.security.StoreAuthorizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/voice")
@Tag(name = "Voz", description = "Interpreta frases faladas ou digitadas")
public class VoiceController {

    private final VoiceService service;
    private final StoreAuthorizationService storeAuth;

    VoiceController(VoiceService service, StoreAuthorizationService storeAuth) {
        this.service = service;
        this.storeAuth = storeAuth;
    }

    /** Só interpreta; o app confirma com o lojista e grava pelos endpoints de venda, compra e despesa. */
    @PostMapping("/interpret")
    public VoiceService.Interpretacao interpretar(@Valid @RequestBody InterpretarRequest request) {
        UUID storeId = request.storeId() != null ? request.storeId() : storeAuth.getCurrentUserStoreId();
        storeAuth.validateStoreAccess(storeId);
        return service.interpretar(storeId, request.text());
    }

    record InterpretarRequest(
        UUID storeId,
        @NotBlank(message = "text is required") String text
    ) {}
}
