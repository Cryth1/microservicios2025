package com.microservice.apigateway.circuitbreaker;

import java.io.IOException;
import java.time.Duration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.support.ServiceUnavailableException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfiguration {


    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> orderCircuitBreakerCustomizer() {
        return factory -> factory.configure(builder -> builder
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                .failureRateThreshold(50)
                                .waitDurationInOpenState(Duration.ofSeconds(10))
                                .slidingWindowSize(5)
                                .build())
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(3)) // 3 segundos
                                .build()),
                "orderCircuitBreaker" // Nombre del Circuit Breaker de tu ruta
        );
    }
}

