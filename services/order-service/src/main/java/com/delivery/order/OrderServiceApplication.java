package com.delivery.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Order Service.
 * 
 * @SpringBootApplication combines:
 *                        - @Configuration: This class can define beans
 *                        - @EnableAutoConfiguration: Spring configures based on
 *                        dependencies in pom.xml
 *                        - @ComponentScan:
 *                        Finds @Controller, @Service, @Repository in this
 *                        package
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
