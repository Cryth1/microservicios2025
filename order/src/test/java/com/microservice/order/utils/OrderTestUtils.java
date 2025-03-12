package com.microservice.order.utils;

import com.microservice.order.model.Order;

import java.util.UUID;

public class OrderTestUtils {
    public static Order createTestOrder() {
        return createTestOrder(
                UUID.randomUUID(),
                3,
                "test." + UUID.randomUUID() + "@example.com",  // Email único por test
                "PENDING"
        );
    }

    public static Order createTestOrder(UUID productId, int quantity, String email, String status) {
        Order order = new Order();
        order.setProduct(productId);
        order.setQuantity(quantity);
        order.setCustomerEmail(email);
        order.setStatus(status);
        return order;
    }
}
