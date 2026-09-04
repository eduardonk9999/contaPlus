package com.contaplus.api.store;

import com.contaplus.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class StoreRepositoryTest {

    @Autowired
    StoreRepository repository;

    @Test
    void deveSalvarERecuperarUmaLoja() {
        Store nova = new Store("Banca do Eduardo");

        repository.save(nova);

        Optional<Store> encontrada = repository.findById(nova.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getName()).isEqualTo("Banca do Eduardo");
    }
}
