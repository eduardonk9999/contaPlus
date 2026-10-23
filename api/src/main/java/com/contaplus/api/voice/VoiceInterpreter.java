package com.contaplus.api.voice;

import java.time.OffsetDateTime;
import java.util.List;

/** Transforma uma frase do lojista em um lançamento estruturado. */
public interface VoiceInterpreter {

    RespostaModelo interpretar(String frase, Contexto contexto);

    record Contexto(
        List<ProdutoCatalogo> produtos,
        List<FormaPagamentoCatalogo> formasPagamento,
        OffsetDateTime agora
    ) {}

    record ProdutoCatalogo(String id, String nome, String unidade) {}

    record FormaPagamentoCatalogo(String id, String nome) {}
}
