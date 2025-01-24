package com.example.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TransactionManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransactionManagementApplication.class, args);
    }
}
