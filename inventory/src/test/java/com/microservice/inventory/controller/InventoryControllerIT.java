package com.microservice.inventory.controller;

import com.microservice.inventory.model.Inventory;
import com.microservice.inventory.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
public class InventoryControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private InventoryRepository repository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Test
    void createInventory_ShouldSucceed() {
        Inventory inventory = new Inventory(null, UUID.randomUUID(), 100, "WAREHOUSE-A", null);

        webTestClient.post()
                .uri("/api/inventory")
                .bodyValue(inventory)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Inventory.class)
                .value(response -> assertEquals(inventory.getProductId(), response.getProductId()));
    }

    @Test
    void updateStock_ShouldFailWhenInsufficientStock() {
        UUID productId = UUID.randomUUID();
        Inventory inventory = new Inventory(null, productId, 5, "STORE-1", 0L);

        // Guardar y forzar flush
        repository.saveAndFlush(inventory); // ¡Clave aquí!
        assertTrue(repository.existsByProductId(productId));

        webTestClient.patch()
                .uri("/api/inventory/product/{productId}/stock?action=subtract&quantity=10", productId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT); // Ahora sí recibirá 409
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }
}