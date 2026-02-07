package com.delivery.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Payment Service.
 * 
 * @SpringBootApplication combines:
 * - @Configuration: This class can define beans
 * - @EnableAutoConfiguration: Spring configures based on dependencies in pom.xml
 * - @ComponentScan: Finds @Controller, @Service, @Repository in this package
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
