package com.microservice.order.controller;

import com.microservice.order.model.Order;
import com.microservice.order.repository.OrderRepository;
import com.microservice.order.utils.OrderTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
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
public class OrderControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }


    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void createOrder_ShouldReturnCreatedOrder() {
        Order order = OrderTestUtils.createTestOrder();

        webTestClient.post()
                .uri("/api/orders")
                .bodyValue(order)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Order.class)
                .value(response -> {
                    assertNotNull(response.getId());
                    assertEquals(order.getStatus(), response.getStatus());
                    assertEquals(order.getQuantity(), response.getQuantity());
                });
    }

    @Test
    void getOrderById_WhenNotExists_ShouldReturnNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        webTestClient.get()
                .uri("/api/orders/{id}", nonExistentId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateOrderStatus_ShouldReturnUpdatedOrder() {
        Order savedOrder = orderRepository.save(OrderTestUtils.createTestOrder());
        Order updateRequest = OrderTestUtils.createTestOrder();
        updateRequest.setStatus("SHIPPED");

        webTestClient.put()
                .uri("/api/orders/{id}", savedOrder.getId())
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Order.class)
                .value(response -> {
                    assertEquals("SHIPPED", response.getStatus());
                    assertEquals(savedOrder.getId(), response.getId());
                });
    }
}
