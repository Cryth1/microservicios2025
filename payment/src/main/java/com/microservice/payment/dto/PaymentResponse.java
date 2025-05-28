package com.microservice.payment.dto;

import com.microservice.payment.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionId;
    private String message;
}