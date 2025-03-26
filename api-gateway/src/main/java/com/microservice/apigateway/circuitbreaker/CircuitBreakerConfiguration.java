package com.microservice.apigateway.circuitbreaker;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreaker;
import java.time.Duration;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // % de fallos para abrir el circuito
                .waitDurationInOpenState(Duration.ofSeconds(10)) // Tiempo en estado abierto
                .slidingWindowSize(5) // Número de llamadas para calcular el umbral
                .build();

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder.Resilience4JCircuitBreakerConfiguration());
    }

}
