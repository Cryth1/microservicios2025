package com.microservice.apigateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class CachingFilter implements GatewayFilter {

    private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory;

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // Inyecta RedisTemplate

    @Autowired
    public CachingFilter(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilterFactory) {
        this.modifyResponseBodyFilterFactory = modifyResponseBodyFilterFactory;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // Solo cachear GET en productos
        if (HttpMethod.GET.name().equals(method) && path.startsWith("/api/products")) {

            // Buscar en Redis
            String cachedResponse = redisTemplate.opsForValue().get(path);

            if (cachedResponse != null) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.OK);
                return response.writeWith(Mono.just(
                        response.bufferFactory().wrap(cachedResponse.getBytes(StandardCharsets.UTF_8))
                ));
            }
        }

        return chain.filter(exchange).then(
                modifyResponseBodyFilterFactory.apply(
                        new ModifyResponseBodyGatewayFilterFactory.Config()
                                .setRewriteFunction(String.class, String.class, (webexchange, body) -> {

                                    if (HttpMethod.GET.name().equals(method)
                                            && path.startsWith("/api/products")
                                            && webexchange.getResponse().getStatusCode() == HttpStatus.OK) {

                                        // Guardar en Redis con TTL de 10 minutos
                                        redisTemplate.opsForValue().set(
                                                path,
                                                body,
                                                10,
                                                TimeUnit.MINUTES
                                        );
                                    }
                                    return Mono.just(body);
                                })
                ).filter(exchange, chain)
        );
    }
}
