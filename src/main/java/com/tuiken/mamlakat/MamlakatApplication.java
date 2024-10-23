package com.tuiken.mamlakat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MamlakatApplication {

	public static void main(String[] args) {
		SpringApplication.run(MamlakatApplication.class, args);
	}

}
