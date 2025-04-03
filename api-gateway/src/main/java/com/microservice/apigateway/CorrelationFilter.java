package com.microservice.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Component
public class CorrelationFilter extends AbstractGatewayFilterFactory<CorrelationFilter.Config> {

    public CorrelationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String correlationId = UUID.randomUUID().toString();

            // Agregar correlationId al request
            exchange.getAttributes().put("correlationId", correlationId);

            exchange.getRequest().mutate()
                    .header("correlationId", correlationId)
                    .build();

            // Agregar correlationId al response
            exchange.getResponse().getHeaders().add("correlationId", correlationId);

            return chain.filter(exchange);
        };
    }

    public static class Config { }
}