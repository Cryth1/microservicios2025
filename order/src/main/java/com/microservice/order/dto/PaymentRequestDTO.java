package com.microservice.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO { // DTO específico para la comunicación desde OrderService
    private UUID orderId;
    private BigDecimal amount;
}