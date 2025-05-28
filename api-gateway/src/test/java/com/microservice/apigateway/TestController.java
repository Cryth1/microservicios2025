package com.microservice.apigateway;

import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

public class TestController {

    private final RedisReactiveCommands<String, String> redisCommands;

    public TestController(RedisReactiveCommands<String, String> redisCommands) {
        this.redisCommands = redisCommands;
    }

    @GetMapping("/test-redis")
    @Test
    public void testRedis() {
        redisCommands.set("test-key", "test-value")
                .flatMap(reply -> redisCommands.get("test-key"))
                .map(value -> "Valor recuperado: " + value);
    }
}
