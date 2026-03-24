package com.example.ngerShop_be;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NgerShopBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(NgerShopBeApplication.class, args);
	}

}
