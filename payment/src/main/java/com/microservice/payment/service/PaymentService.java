package com.microservice.payment.service;

import com.microservice.payment.dto.PaymentRequest;
import com.microservice.payment.dto.PaymentResponse;
import com.microservice.payment.model.Payment;
import com.microservice.payment.model.PaymentStatus;
import com.microservice.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Mono<PaymentResponse> processPayment(PaymentRequest paymentRequest) {
        return Mono.fromCallable(() -> {
            log.info("Processing payment for order ID: {}", paymentRequest.getOrderId());

            // Simulación de lógica de pago
            boolean paymentSuccess = Math.random() > 0.1; // 90% de éxito

            Payment payment = Payment.builder()
                    .orderId(paymentRequest.getOrderId())
                    .amount(paymentRequest.getAmount())
                    .status(paymentSuccess ? PaymentStatus.SUCCESSFUL : PaymentStatus.FAILED)
                    .transactionId(paymentSuccess ? "txn_" + UUID.randomUUID().toString().substring(0, 8) : null)
                    .build();

            paymentRepository.save(payment);

            log.info("Payment for order ID: {} processed with status: {}", payment.getOrderId(), payment.getStatus());

            return new PaymentResponse(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getAmount(),
                    payment.getStatus(),
                    payment.getTransactionId(),
                    paymentSuccess ? "Payment successful" : "Payment failed"
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<PaymentResponse> refundPayment(UUID orderId) {
        return Mono.fromCallable(() -> {
            log.info("Processing refund for order ID: {}", orderId);
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Payment not found for order ID: " + orderId + " to refund."));

            if (payment.getStatus() == PaymentStatus.SUCCESSFUL) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                log.info("Refund for order ID: {} successful.", orderId);
                return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getStatus(), payment.getTransactionId(), "Refund successful");
            } else {
                log.warn("Cannot refund payment for order ID: {}. Current status: {}", orderId, payment.getStatus());
                return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getStatus(), payment.getTransactionId(), "Refund not possible or already refunded");
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}