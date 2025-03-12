package com.microservice.order.repository;

import com.microservice.order.model.Order;
import com.microservice.order.utils.OrderTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Testcontainers
public class OrderRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Test
    void shouldSaveAndRetrieveOrder() {
        // 1. Crear un nuevo pedido
        Order order = OrderTestUtils.createTestOrder();

        // 2. Guardar en BD
        Order savedOrder = orderRepository.save(order);

        // 3. Recuperar de BD
        Order retrievedOrder = orderRepository.findById(savedOrder.getId())
                .orElseThrow();

        // 4. Verificaciones
        assertNotNull(savedOrder.getId(), "El ID debería generarse automáticamente");
        assertEquals(order.getProduct(), retrievedOrder.getProduct(), "El producto debe coincidir");
        assertEquals(order.getQuantity(), retrievedOrder.getQuantity(), "La cantidad debe coincidir");
        assertEquals(order.getCustomerEmail(), retrievedOrder.getCustomerEmail(), "El email debe coincidir");
        assertEquals(order.getStatus(), retrievedOrder.getStatus(), "El estado debe coincidir");
    }
    @Test
    void shouldReturnEmptyWhenOrderNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        assertTrue(orderRepository.findById(nonExistentId).isEmpty());
    }

    @Test
    void shouldDeleteOrder() {

        Order order = OrderTestUtils.createTestOrder();

        Order orderToDelete = orderRepository.save(order);


        orderRepository.deleteById(orderToDelete.getId());
        assertFalse(orderRepository.existsById(orderToDelete.getId()));
    }

    @Test
    void shouldUpdateOrderStatus() {
        // Setup
        Order order = orderRepository.save(OrderTestUtils.createTestOrder());
        String newStatus = "SHIPPED";

        // Act
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // Assert
        assertEquals(newStatus, updatedOrder.getStatus());
    }

    @Test
    void shouldFailWhenSavingInvalidOrder() {
        Order invalidOrder = new Order();
        // Campos obligatorios sin setear: product, quantity, etc.

        assertThrows(DataIntegrityViolationException.class, () -> {
            orderRepository.saveAndFlush(invalidOrder);
        });
    }




}
