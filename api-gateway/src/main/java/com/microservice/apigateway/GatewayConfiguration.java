package com.microservice.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.UUID;
@Configuration
public class GatewayConfiguration {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, CachingFilter cachingFilter) {
        return builder.routes()
                .route("order-service", r -> r.path("/api/orders/**")
                        .filters(f -> f
                                .rewritePath("/api/orders/(?<segment>.*)", "/orders/${segment}")
                                .retry(retryConfig -> retryConfig // CONFIGURACIÓN DE RETRY
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY)
                                        .setBackoff(Duration.ofMillis(500), Duration.ofMillis(2000), 2, true)
                                )
                                .circuitBreaker(c -> c.setName("orderCircuitBreaker").setFallbackUri("forward:/orderFallback"))
                                .addRequestHeader("correlationId", UUID.randomUUID().toString())
                        )
                        .uri("lb://order-service"))
                .route("inventory", r -> r.path("/api/inventory/**")
                        .filters(f -> f
                                .rewritePath("/api/inventory/(?<segment>.*)", "/inventory/${segment}") // MODIFICACIÓN
                                .retry(retryConfig -> retryConfig // CONFIGURACIÓN DE RETRY
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY)
                                        .setBackoff(Duration.ofMillis(500), Duration.ofMillis(2000), 2, true)
                                )
                                .circuitBreaker(c -> c.setName("inventoryCircuitBreaker").setFallbackUri("forward:/inventoryFallback"))
                                .addResponseHeader("correlationId", UUID.randomUUID().toString())
                        )
                        .uri("lb://inventory"))
                .route("product", r -> r.path("/api/products/**")
                        .filters(f -> f
                                .rewritePath("/api/products/(?<segment>.*)", "/products/${segment}") // MODIFICACIÓN
                                .retry(retryConfig -> retryConfig // CONFIGURACIÓN DE RETRY
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                                        .setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY)
                                        .setBackoff(Duration.ofMillis(500), Duration.ofMillis(2000), 2, true)
                                )
                                .filter(cachingFilter)
                        )
                        .uri("lb://product"))
                .build();
    }
}