package com.microservice.order.controller;

import com.microservice.order.model.Order;
import com.microservice.order.model.OrderStatus;
import com.microservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // O HttpStatus.ACCEPTED si el procesamiento es largo y asíncrono
    public Mono<Order> createOrder(@RequestBody Order orderRequest) { // El request body podría ser un DTO más específico
        // El orderRequest DTO debería tener productId, quantity, customerEmail.
        // El servicio se encarga de construir el objeto Order completo.
        return orderService.createOrder(orderRequest);
    }

    @GetMapping
    public Flux<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Order>> getOrderById(@PathVariable UUID id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public Flux<Order> getOrdersByStatus(@PathVariable String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return orderService.getAllOrders() // Idealmente, el servicio tendría un métodofindByStatus
                    .filter(order -> order.getStatus() == orderStatus);
        } catch (IllegalArgumentException e) {
            return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status: " + status));
        }
    }

    @PutMapping("/{id}") // Usar PATCH para actualizaciones parciales
    public Mono<ResponseEntity<Order>> updateOrderStatus(@PathVariable UUID id, @RequestParam("status") String status) {
        try {
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
            Order orderUpdates = new Order();
            orderUpdates.setStatus(newStatus);
            return orderService.updateOrder(id, orderUpdates)
                    .map(ResponseEntity::ok)
                    .defaultIfEmpty(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteOrder(@PathVariable UUID id) {
        // Considerar la lógica de negocio. Usualmente las órdenes se cancelan, no se borran.
        return orderService.deleteOrder(id);
    }

    // Endpoints de prueba de lentitud y error (mantener si son útiles para pruebas de resiliencia)
    @GetMapping("/slow")
    public Mono<String> slowEndpoint() {
        return Mono.delay(java.time.Duration.ofSeconds(3)).thenReturn("OK");
    }

    @GetMapping("/error")
    public Mono<String> errorEndpoint() {
        return Mono.error(new RuntimeException("Fallo simulado en OrderController"));
    }
}