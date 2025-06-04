package com.microservice.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class GatewayConfiguration {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, CachingFilter cachingFilter) {
        return builder.routes()
                // 1. Configuración para Order Service
                .route("order-service", r -> r
                        .path(
                                "/api/orders",
                                "/api/orders/",
                                "/api/orders/**"
                        )
                        .filters(f -> f
                                .rewritePath("/api/orders/?", "/orders") // Maneja /api/orders y /api/orders/
                                .rewritePath("/api/orders/(?<segment>.*)", "/orders/${segment}") // Maneja subrutas
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
                                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY)
                                        .setBackoff(Duration.ofMillis(500), Duration.ofMillis(2000), 2, true)
                                )
                                .circuitBreaker(c -> c
                                        .setName("orderCircuitBreaker")
                                        .setFallbackUri("forward:/orderFallback"))
                                .addRequestHeader("correlationId", UUID.randomUUID().toString())
                                .addResponseHeader("correlationId", UUID.randomUUID().toString())
                        )
                        .uri("lb://order-service"))

                // 2. Configuración para Inventory Service
                .route("inventory-service", r -> r
                        .path(
                                "/api/inventory",
                                "/api/inventory/",
                                "/api/inventory/**"
                        )
                        .filters(f -> f
                                .rewritePath("/api/inventory/?", "/inventory") // Maneja ruta base
                                .rewritePath("/api/inventory/(?<segment>.*)", "/inventory/${segment}") // Subrutas
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PATCH)
                                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY)
                                        .setBackoff(Duration.ofMillis(500), Duration.ofMillis(2000), 2, true)
                                )
                                .circuitBreaker(c -> c
                                        .setName("inventoryCircuitBreaker")
                                        .setFallbackUri("forward:/inventoryFallback"))
                                .addRequestHeader("correlationId", UUID.randomUUID().toString())
                                .addResponseHeader("correlationId", UUID.randomUUID().toString())
                        )
                        .uri("lb://inventory"))

                // 3. Configuración para Product Service (con CachingFilter)
                .route("product-service", r -> r
                        .path(
                                "/api/products",
                                "/api/products/",
                                "/api/products/**"
                        )
                        .filters(f -> f
                                .rewritePath("/api/products/?", "/products") // Ruta base
                                .rewritePath("/api/products/(?<segment>.*)", "/products/${segment}") // Subrutas
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY)
                                        .setBackoff(Duration.ofMillis(500), Duration.ofMillis(2000), 2, true)
                                )
                                .filter(cachingFilter) // Filtro de caché personalizado
                                .addRequestHeader("correlationId", UUID.randomUUID().toString())
                                .addResponseHeader("correlationId", UUID.randomUUID().toString())
                        )
                        .uri("lb://product"))

                // 4. Configuración para Payment Service
                .route("payment-service", r -> r
                        .path(
                                "/api/payments",
                                "/api/payments/",
                                "/api/payments/**"
                        )
                        .filters(f -> f
                                .rewritePath("/api/payments/?", "/payments") // Ruta base
                                .rewritePath("/api/payments/(?<segment>.*)", "/payments/${segment}") // Subrutas
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setMethods(HttpMethod.POST)
                                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY)
                                        .setBackoff(Duration.ofMillis(500), Duration.ofMillis(2000), 2, true)
                                )
                                .circuitBreaker(c -> c
                                        .setName("paymentCircuitBreaker")
                                        .setFallbackUri("forward:/paymentFallback"))
                                .addRequestHeader("correlationId", UUID.randomUUID().toString())
                                .addResponseHeader("correlationId", UUID.randomUUID().toString())
                        )
                        .uri("lb://payment-service"))
                .build();
    }
}