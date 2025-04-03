package com.microservice.apigateway;

import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class GatewayConfiguration {

    @Bean
    public CachingFilter cachingFilter(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory) {
        return new CachingFilter(modifyResponseBodyFilterFactory);
    }
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, CachingFilter cachingFilter) {
        return builder.routes()
                .route("order-service", r -> r.path("/api/orders/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c.setName("orderCircuitBreaker").setFallbackUri("forward:/orderFallback"))
                                .addRequestHeader("correlationId", UUID.randomUUID().toString())
                        )
                        .uri("lb://order-service"))
                .route("inventory", r -> r.path("/api/inventory/**")
                        .filters(f -> f
                                .circuitBreaker(c -> c.setName("inventoryCircuitBreaker").setFallbackUri("forward:/inventoryFallback"))
                                .addResponseHeader("correlationId", UUID.randomUUID().toString())
                        )
                        .uri("lb://inventory"))
                .route("product", r -> r.path("/api/products/**")
                        .filters(f -> f.filter(cachingFilter)) // Usar el bean en lugar de una nueva instancia
                        .uri("lb://product"))
                .build();
    }
}