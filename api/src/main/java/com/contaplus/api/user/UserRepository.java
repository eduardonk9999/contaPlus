package com.contaplus.api.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // O usuário autenticado vive fora da sessão do Hibernate (open-in-view: false);
    // a loja precisa vir carregada junto para ser lida nos controllers.
    @EntityGraph(attributePaths = "store")
    Optional<User> findWithStoreById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);
}
