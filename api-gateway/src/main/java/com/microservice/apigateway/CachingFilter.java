package com.microservice.apigateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CachingFilter implements GatewayFilter {
    private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory;
    private static final ConcurrentHashMap<String, ResponseEntity<String>> cache = new ConcurrentHashMap<>();

    @Autowired
    public CachingFilter(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory) {
        this.modifyResponseBodyFilterFactory = modifyResponseBodyFilterFactory;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (exchange.getRequest().getMethod() == HttpMethod.GET && path.startsWith("/api/products") && cache.containsKey(path)) {
            ResponseEntity<String> cachedResponse = cache.get(path);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(cachedResponse.getStatusCode());
            response.getHeaders().putAll(cachedResponse.getHeaders());
            return response.writeWith(Mono.just(response.bufferFactory().wrap(Objects.requireNonNull(cachedResponse.getBody()).getBytes(StandardCharsets.UTF_8))));
        }

        return chain.filter(exchange).then(Mono.defer(() -> modifyResponseBodyFilterFactory.apply(new ModifyResponseBodyGatewayFilterFactory.Config()
                .setRewriteFunction(String.class, String.class, (serverWebExchange, body) -> {
                    if (serverWebExchange.getResponse().getStatusCode() == HttpStatus.OK && body != null) {
                        cache.put(path, ResponseEntity.ok().body(body));
                    }
                    assert body != null;
                    return Mono.just(body);
                })).filter(exchange, chain)));
    }
}