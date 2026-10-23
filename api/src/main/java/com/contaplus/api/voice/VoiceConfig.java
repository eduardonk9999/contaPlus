package com.contaplus.api.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VoiceConfig {

    /**
     * Sem ANTHROPIC_API_KEY a API sobe normalmente e /v1/voice/interpret
     * responde 503; o app cai no cadastro manual.
     */
    @Bean
    VoiceInterpreter voiceInterpreter(@Value("${voice.anthropic-api-key:}") String apiKey,
                                      @Value("${voice.model:claude-opus-5-5}") String model) {
        if (apiKey == null || apiKey.isBlank()) {
            return (frase, contexto) -> {
                throw new VoiceUnavailableException("Lançamento por voz não configurado no servidor.");
            };
        }
        return new ClaudeVoiceInterpreter(apiKey, model);
    }
}
