package com.contaplus.api;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Os testes de integração compartilham o mesmo Postgres (o contexto do Spring fica em cache),
 * então cada teste começa apagando o que os outros deixaram — em qualquer ordem de execução.
 */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void limpar(JdbcTemplate jdbc) {
        List<String> tabelas = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'",
                String.class
        );
        if (!tabelas.isEmpty()) {
            jdbc.execute("TRUNCATE TABLE " + String.join(", ", tabelas) + " CASCADE");
        }
    }
}
