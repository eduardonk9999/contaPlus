package com.contaplus.api.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface StoreRepository extends JpaRepository<Store, UUID> {
}
