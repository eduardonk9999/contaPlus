package com.contaplus.api.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByStore_IdAndActiveTrue(UUID storeId);

    List<Customer> findByStore_Id(UUID storeId);

    Page<Customer> findByStore_IdAndActiveTrue(UUID storeId, Pageable pageable);

    Page<Customer> findByStore_Id(UUID storeId, Pageable pageable);

    Page<Customer> findByStore_IdAndNameContainingIgnoreCaseAndActiveTrue(UUID storeId, String name, Pageable pageable);

    Optional<Customer> findByStore_IdAndPhone(UUID storeId, String phone);

    Optional<Customer> findByStore_IdAndCpfCnpj(UUID storeId, String cpfCnpj);

    List<Customer> findByStore_IdAndNameContainingIgnoreCase(UUID storeId, String name);
}
