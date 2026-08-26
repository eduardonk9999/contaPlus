package com.contaplus.api.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "stores_")
public class Store {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String currency;



}
