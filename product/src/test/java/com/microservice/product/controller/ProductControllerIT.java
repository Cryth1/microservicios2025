package com.microservice.product.controller;

import com.microservice.product.model.Product;
import com.microservice.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class ProductControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProductRepository repository;

    @Test
    void createProduct_ShouldReturn201() {
        Product product = Product.builder()
                .sku("SKU-TEST")
                .name("Test Product")
                .price(BigDecimal.TEN)
                .build();

        webTestClient.post()
                .uri("/api/products")
                .bodyValue(product)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Product.class)
                .value(p -> assertNotNull(p.getProductId()));
    }
}