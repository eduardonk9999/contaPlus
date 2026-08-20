package com.contaplus.api;

import org.springframework.boot.SpringApplication;

public class TestContaPlusApplication {

	public static void main(String[] args) {
		SpringApplication.from(ContaPlusApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
