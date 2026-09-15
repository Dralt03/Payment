package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Payment Platform service.
 *
 * @SpringBootApplication is a shortcut for three annotations:
 *   @Configuration       - this class can define Spring beans
 *   @EnableAutoConfiguration - Spring Boot auto-configures JPA, Flyway, Web, etc.
 *   @ComponentScan       - scans this package and sub-packages for @Service, @Repository, etc.
 *
 * @EnableScheduling activates Spring's task scheduling engine,
 * which is needed for our PaymentSchedulerRunner (@Scheduled methods).
 */
@SpringBootApplication
@EnableScheduling
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
