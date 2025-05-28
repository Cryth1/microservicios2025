package com.microservice.order.service;

import java.util.UUID;

public class PaymentRequestDTO {
    private UUID orderId;
    private Double amount;
    private String paymentMethod;
    // Getters y setters
}
