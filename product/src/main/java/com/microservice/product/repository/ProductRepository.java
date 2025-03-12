package com.microservice.product.repository;

import com.microservice.product.model.Product;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import reactor.core.publisher.Mono;
import java.util.UUID;


public interface ProductRepository extends ReactiveMongoRepository<Product, String> {
    Mono<Product> findByProductId(UUID productId);
    Mono<Boolean> existsBySku(String sku);
    Mono<Void> deleteByProductId(UUID productId);
}
