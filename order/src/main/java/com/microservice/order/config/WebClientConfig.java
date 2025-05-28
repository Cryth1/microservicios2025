package com.microservice.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced // Habilita el balanceo de carga del lado del cliente (usa nombres de servicio de Eureka)
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}