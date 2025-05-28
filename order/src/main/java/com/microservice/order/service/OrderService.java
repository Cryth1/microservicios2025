package com.microservice.order.service;

import com.microservice.order.dto.InventoryUpdateResponseDTO;
import com.microservice.order.dto.PaymentRequestDTO;
import com.microservice.order.dto.PaymentResponseDTO;
import com.microservice.order.dto.ProductDTO;
import com.microservice.order.model.Order;
import com.microservice.order.model.OrderStatus;
import com.microservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder; // Inyecta el builder configurado

    // Nombres de servicio registrados en Eureka
    private final String PRODUCT_SERVICE_URL = "http://product-service/products";
    private final String INVENTORY_SERVICE_URL = "http://inventory-service/inventory";
    private final String PAYMENT_SERVICE_URL = "http://payment-service/payments";

    @Transactional // Esta transacción es local a OrderService
    public Mono<Order> createOrder(Order orderRequest) {
        // 1. Guardar orden inicial con estado PENDING_VALIDATION
        Order newOrder = Order.builder()
                .productId(orderRequest.getProductId())
                .quantity(orderRequest.getQuantity())
                .customerEmail(orderRequest.getCustomerEmail())
                .status(OrderStatus.PENDING_VALIDATION)
                .build();

        return Mono.fromCallable(() -> orderRepository.save(newOrder))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(savedOrder -> {
                    log.info("Order {} created with status PENDING_VALIDATION", savedOrder.getId());
                    // 2. Validar producto y obtener precio
                    return getProductDetails(savedOrder.getProductId())
                            .flatMap(productDTO -> {
                                log.info("Product {} details fetched: Price {}", productDTO.getProductId(), productDTO.getPrice());
                                savedOrder.setTotalAmount(productDTO.getPrice().multiply(BigDecimal.valueOf(savedOrder.getQuantity())));
                                // 3. Intentar actualizar stock
                                return updateInventoryStock(savedOrder.getProductId(), savedOrder.getQuantity(), "subtract")
                                        .flatMap(inventoryResponse -> {
                                            log.info("Stock for product {} updated successfully.", savedOrder.getProductId());
                                            savedOrder.setStatus(OrderStatus.AWAITING_PAYMENT);
                                            return Mono.fromCallable(() -> orderRepository.save(savedOrder))
                                                    .subscribeOn(Schedulers.boundedElastic())
                                                    .flatMap(orderAwaitingPayment -> {
                                                        // 4. Procesar pago
                                                        return processPayment(orderAwaitingPayment.getId(), orderAwaitingPayment.getTotalAmount())
                                                                .flatMap(paymentResponse -> {
                                                                    if ("SUCCESSFUL".equalsIgnoreCase(paymentResponse.getStatus())) {
                                                                        log.info("Payment successful for order {}", orderAwaitingPayment.getId());
                                                                        orderAwaitingPayment.setStatus(OrderStatus.CONFIRMED);
                                                                    } else {
                                                                        log.error("Payment failed for order {}. Initiating stock revert.", orderAwaitingPayment.getId());
                                                                        orderAwaitingPayment.setStatus(OrderStatus.CANCELLED_PAYMENT_FAILED);
                                                                        // Compensación: Revertir stock
                                                                        return updateInventoryStock(orderAwaitingPayment.getProductId(), orderAwaitingPayment.getQuantity(), "add")
                                                                                .doOnSuccess(revert -> log.info("Stock reverted for order {}", orderAwaitingPayment.getId()))
                                                                                .doOnError(err -> log.error("Failed to revert stock for order {}", orderAwaitingPayment.getId(), err))
                                                                                .thenReturn(orderAwaitingPayment); // Devuelve la orden con estado CANCELLED_PAYMENT_FAILED
                                                                    }
                                                                    return Mono.fromCallable(() -> orderRepository.save(orderAwaitingPayment)).subscribeOn(Schedulers.boundedElastic());
                                                                })
                                                                .onErrorResume(paymentError -> { // Error en la llamada de pago
                                                                    log.error("Error calling payment service for order {}: {}. Initiating stock revert.", orderAwaitingPayment.getId(), paymentError.getMessage());
                                                                    orderAwaitingPayment.setStatus(OrderStatus.CANCELLED_PAYMENT_FAILED);
                                                                    // Compensación: Revertir stock
                                                                    return updateInventoryStock(orderAwaitingPayment.getProductId(), orderAwaitingPayment.getQuantity(), "add")
                                                                            .doOnSuccess(revert -> log.info("Stock reverted after payment call error for order {}", orderAwaitingPayment.getId()))
                                                                            .doOnError(err -> log.error("Failed to revert stock after payment call error for order {}", orderAwaitingPayment.getId(), err))
                                                                            .then(Mono.fromCallable(() -> orderRepository.save(orderAwaitingPayment)).subscribeOn(Schedulers.boundedElastic()));
                                                                });
                                                    });
                                        })
                                        .onErrorResume(InventoryException.class, e -> { // Error específico de inventario
                                            log.warn("Inventory update failed for product {}: {}", savedOrder.getProductId(), e.getMessage());
                                            savedOrder.setStatus(OrderStatus.CANCELLED_NO_STOCK);
                                            return Mono.fromCallable(() -> orderRepository.save(savedOrder)).subscribeOn(Schedulers.boundedElastic());
                                        });
                            })
                            .onErrorResume(ProductNotFoundException.class, e -> { // Error específico de producto
                                log.warn("Product not found: {}", savedOrder.getProductId());
                                savedOrder.setStatus(OrderStatus.CANCELLED_PRODUCT_NOT_FOUND);
                                return Mono.fromCallable(() -> orderRepository.save(savedOrder)).subscribeOn(Schedulers.boundedElastic());
                            })
                            .onErrorResume(e -> { // Captura general para otros errores en el flujo principal (ej. WebClientException no manejado específicamente)
                                log.error("Generic error during order processing for order (to be) {}: {}", savedOrder.getId(), e.getMessage(), e);
                                savedOrder.setStatus(OrderStatus.CANCELLED_PAYMENT_FAILED); // O un estado más genérico de error
                                // Considerar si se debe intentar revertir stock aquí también si ya pasó esa etapa
                                return Mono.fromCallable(() -> orderRepository.save(savedOrder)).subscribeOn(Schedulers.boundedElastic());
                            });
                });
    }


    private Mono<ProductDTO> getProductDetails(UUID productId) {
        return webClientBuilder.build().get()
                .uri(PRODUCT_SERVICE_URL + "/{productId}", productId)
                .retrieve()
                .bodyToMono(ProductDTO.class)
                .onErrorMap(ex -> new ProductNotFoundException("Product not found: " + productId + ". Error: " + ex.getMessage()));
    }

    private Mono<InventoryUpdateResponseDTO> updateInventoryStock(UUID productId, int quantity, String action) {
        // Asumimos que el inventory service devuelve un DTO simple o el objeto Inventory.
        // Para este ejemplo, vamos a asumir que devuelve un DTO o simplemente un 200 OK.
        // Si devuelve el objeto Inventory, puedes mapearlo a InventoryUpdateResponseDTO.
        // Nota: InventoryController devuelve Mono<Inventory> o Mono<String> en errores.
        // Es mejor que devuelva DTOs consistentes o códigos HTTP claros.
        return webClientBuilder.build().patch()
                .uri(INVENTORY_SERVICE_URL + "/product/{productId}/stock?action={action}&quantity={quantity}",
                        productId, action, quantity)
                .retrieve()
                .bodyToMono(InventoryUpdateResponseDTO.class) // Si InventoryService devuelve un DTO

                .map(inventory -> { // Mapear a un DTO común si es necesario, o manejar aquí.
                    log.info("Inventory service responded for product {}: new quantity {}", inventory.getProductId(), inventory.getNewQuantity());
                    InventoryUpdateResponseDTO dto = new InventoryUpdateResponseDTO();
                    dto.setProductId(inventory.getProductId());
                    dto.setNewQuantity(inventory.getNewQuantity());
                    dto.setMessage("Stock updated successfully");
                    return dto;
                })
                .onErrorMap(ex -> { // Mapear errores de WebClient a excepciones de negocio
                    log.error("Error calling inventory service for product {}: {}", productId, ex.getMessage());
                    return new InventoryException("Failed to update inventory for product " + productId + ". Error: " + ex.getMessage());
                });
    }

    private Mono<PaymentResponseDTO> processPayment(UUID orderId, BigDecimal amount) {
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO(orderId, amount);
        return webClientBuilder.build().post()
                .uri(PAYMENT_SERVICE_URL + "/process")
                .bodyValue(paymentRequest)
                .retrieve()
                .bodyToMono(PaymentResponseDTO.class)
                .onErrorMap(ex -> new PaymentException("Payment processing failed for order " + orderId + ". Error: " + ex.getMessage()));
    }


    // Excepciones personalizadas para el flujo de órdenes
    static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message) { super(message); }
    }
    static class InventoryException extends RuntimeException {
        public InventoryException(String message) { super(message); }
    }
    static class PaymentException extends RuntimeException {
        public PaymentException(String message) { super(message); }
    }


    // --- Métodos CRUD existentes (adaptados para el nuevo OrderStatus y builder) ---
    public Flux<Order> getAllOrders() {
        return Flux.fromIterable(orderRepository.findAll())
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Order> getOrderById(UUID id) {
        return Mono.fromCallable(() -> orderRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Order not found with id: " + id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<Order> updateOrder(UUID id, Order orderDetails) { // Usado para actualizar estado, etc.
        return Mono.fromCallable(() -> {
                    Order order = orderRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));

                    // Solo permitir actualizar ciertos campos, por ejemplo, el estado
                    if (orderDetails.getStatus() != null) {
                        order.setStatus(orderDetails.getStatus());
                    }
                    // Añadir más campos actualizables si es necesario
                    // order.setQuantity(orderDetails.getQuantity()); // Cuidado con esto si afecta el flujo de negocio

                    return orderRepository.save(order);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<Void> deleteOrder(UUID id) {
        // Considerar la lógica de negocio antes de borrar una orden.
        // ¿Se deben revertir pagos/inventario? Generalmente, las órdenes se cancelan, no se borran.
        return Mono.fromRunnable(() -> orderRepository.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}