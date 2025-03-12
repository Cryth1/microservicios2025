package com.microservice.order.service;

import com.microservice.order.model.Order;
import com.microservice.order.repository.OrderRepository;
import lombok.*;
import reactor.core.publisher.*;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
@RequiredArgsConstructor

public class OrderService {
    private final OrderRepository orderRepository;

    public Mono<Order> createOrder(Order order) {
        return Mono.fromSupplier(() -> orderRepository.save(order))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Order> getAllOrders() {
        return Flux.fromIterable(orderRepository.findAll())
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Order> getOrderById(UUID id) {
        return Mono.fromCallable(() -> orderRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Order not found")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Order> updateOrder(UUID id, Order orderDetails) {
        return Mono.fromSupplier(() -> {
                    Order order = orderRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Order not found"));

                    order.setQuantity(orderDetails.getQuantity());
                    order.setStatus(orderDetails.getStatus());
                    return orderRepository.save(order);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteOrder(UUID id) {
        return Mono.fromRunnable(() -> orderRepository.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
