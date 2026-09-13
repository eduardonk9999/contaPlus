package com.contaplus.api.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByStore_IdAndActiveTrue(UUID storeId);

    List<Customer> findByStore_Id(UUID storeId);

    Optional<Customer> findByStore_IdAndPhone(UUID storeId, String phone);

    Optional<Customer> findByStore_IdAndCpfCnpj(UUID storeId, String cpfCnpj);

    List<Customer> findByStore_IdAndNameContainingIgnoreCase(UUID storeId, String name);
}
