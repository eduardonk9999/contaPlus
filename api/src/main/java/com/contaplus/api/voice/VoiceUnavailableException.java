package com.contaplus.api.voice;

/** Interpretação por voz fora do ar: sem chave configurada ou falha ao falar com o Claude. */
public class VoiceUnavailableException extends RuntimeException {
    public VoiceUnavailableException(String message) {
        super(message);
    }

    public VoiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
