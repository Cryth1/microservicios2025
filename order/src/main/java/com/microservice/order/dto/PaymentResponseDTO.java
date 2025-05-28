package com.microservice.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentResponseDTO {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private String status; // "SUCCESSFUL", "FAILED" (simplificado)
    private String transactionId;
    private String message;
}