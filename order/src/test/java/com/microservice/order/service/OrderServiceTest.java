package com.microservice.order.service;

import com.microservice.order.model.Order;
import com.microservice.order.repository.OrderRepository;
import com.microservice.order.utils.OrderTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_ShouldReturnSavedOrder() {
        Order order = OrderTestUtils.createTestOrder();
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.createOrder(order).block();

        assertNotNull(result);
        verify(orderRepository).save(order);
    }

    @Test
    void getOrderById_WhenOrderExists_ShouldReturnOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = OrderTestUtils.createTestOrder();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.getOrderById(orderId).blockOptional();

        assertTrue(result.isPresent());
        assertEquals(order.getId(), result.get().getId());
    }

    @Test
    void deleteOrder_ShouldCallRepository() {
        UUID orderId = UUID.randomUUID();
        doNothing().when(orderRepository).deleteById(orderId);

        Mono<Void> result = orderService.deleteOrder(orderId);

        StepVerifier.create(result)
                .verifyComplete();

        verify(orderRepository).deleteById(orderId);
    }
}
