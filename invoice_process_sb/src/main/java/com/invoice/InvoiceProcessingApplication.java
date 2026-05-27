package com.invoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Invoice Processing System - Phase 1
 * AI-Powered Invoice Extraction with Spring Boot 3.x + Java 21
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class InvoiceProcessingApplication {
    public static void main(String[] args) {
        SpringApplication.run(InvoiceProcessingApplication.class, args);
    }
}

