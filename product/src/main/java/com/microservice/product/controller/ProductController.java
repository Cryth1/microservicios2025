package com.microservice.product.controller;

import com.microservice.product.exception.DuplicateProductException;
import com.microservice.product.exception.ProductNotFoundException;
import com.microservice.product.model.Product;
import com.microservice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }

    @GetMapping
    public Flux<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{productId}")
    public Mono<Product> getProductById(@PathVariable UUID productId) {
        return productService.getProductById(productId);
    }

    @PutMapping("/{productId}")
    public Mono<Product> updateProduct(
            @PathVariable UUID productId,
            @RequestBody Product productDetails
    ) {
        return productService.updateProduct(productId, productDetails);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProduct(@PathVariable UUID productId) {
        return productService.deleteProduct(productId);
    }

    // Manejo de excepciones
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<String> handleProductNotFound(ProductNotFoundException ex) {
        return Mono.just(ex.getMessage());
    }

    @ExceptionHandler(DuplicateProductException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<String> handleDuplicateProduct(DuplicateProductException ex) {
        return Mono.just(ex.getMessage());
    }
}