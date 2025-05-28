package com.microservice.payment.controller;

import com.microservice.payment.dto.PaymentRequest;
import com.microservice.payment.dto.PaymentResponse;
import com.microservice.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PaymentResponse> processPayment(@RequestBody PaymentRequest paymentRequest) {
        return paymentService.processPayment(paymentRequest);
    }

    @PostMapping("/refund/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<PaymentResponse> refundPayment(@PathVariable UUID orderId) {
        // En un caso real, se necesitaría más información o validación para un reembolso
        return paymentService.refundPayment(orderId);
    }
}
