package com.microservice.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private UUID orderId;
    private BigDecimal amount;
    // private String paymentMethodToken; // Ejemplo de otros campos
}