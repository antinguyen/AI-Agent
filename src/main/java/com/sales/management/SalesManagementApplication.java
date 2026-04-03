package com.sales.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SalesManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesManagementApplication.class, args);
    }
}
