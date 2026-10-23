package com.contaplus.api.auth;

import com.contaplus.api.DatabaseCleaner;
import com.contaplus.api.TestcontainersConfiguration;
import com.contaplus.api.payment.PaymentMethod;
import com.contaplus.api.payment.PaymentMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthServiceTest {

    @Autowired
    AuthService authService;

    @Autowired
    PaymentMethodRepository paymentMethodRepository;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.limpar(jdbc);
    }

    @Test
    void cadastroCriaAsFormasDePagamentoPadraoDaLoja() {
        AuthResponse resposta = authService.register(
            new RegisterRequest("dono@loja.com", "senha123", "Dono", null, "Loja Nova"));

        assertThat(paymentMethodRepository.findByStore_IdAndActiveTrue(resposta.user().storeId()))
            .extracting(PaymentMethod::getName)
            .containsExactlyInAnyOrder("Dinheiro", "PIX", "Cartão de Crédito", "Cartão de Débito");
    }
}
