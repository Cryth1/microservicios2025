package com.microservice.product.repository;

import com.microservice.product.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

@DataMongoTest
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Test
    void shouldSaveAndFindByProductId() {
        Product product = Product.builder()
                .productId(UUID.randomUUID())
                .sku("SKU-123")
                .name("Laptop")
                .price(BigDecimal.valueOf(999.99))
                .build();

        repository.save(product).block();

        Mono<Product> found = repository.findByProductId(product.getProductId());

        StepVerifier.create(found)
                .expectNextMatches(p -> p.getSku().equals("SKU-123"))
                .verifyComplete();
    }
}
