package com.microservice.product.service;
import com.microservice.product.exception.DuplicateProductException;
import com.microservice.product.exception.ProductNotFoundException;
import com.microservice.product.model.Product;
import com.microservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    public Mono<Product> createProduct(Product product) {
        return repository.existsBySku(product.getSku())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new DuplicateProductException("SKU ya registrado: " + product.getSku()));
                    }
                    product.setProductId(UUID.randomUUID());
                    return repository.save(product);
                });
    }

    public Flux<Product> getAllProducts() {
        return repository.findAll();
    }

    public Mono<Product> getProductById(UUID productId) {
        return repository.findByProductId(productId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException("Producto no encontrado")));
    }

    public Mono<Product> updateProduct(UUID productId, Product productDetails) {
        return repository.findByProductId(productId)
                .flatMap(existing -> {
                    existing.setName(productDetails.getName());
                    existing.setDescription(productDetails.getDescription());
                    existing.setPrice(productDetails.getPrice());
                    return repository.save(existing);
                })
                .switchIfEmpty(Mono.error(new ProductNotFoundException("Producto no encontrado")));
    }

    public Mono<Void> deleteProduct(UUID productId) {
        return repository.deleteByProductId(productId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException("Producto no encontrado")));
    }
}