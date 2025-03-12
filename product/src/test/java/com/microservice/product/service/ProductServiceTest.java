package com.microservice.product.service;

import com.microservice.product.exception.DuplicateProductException;
import com.microservice.product.model.Product;
import com.microservice.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static reactor.core.publisher.Mono.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    @Test
    void createProduct_ShouldThrowOnDuplicateSku() {
        Product product = Product.builder().sku("DUPE-123").build();

        when(repository.existsBySku("DUPE-123")).thenReturn(Mono.just(true));

        StepVerifier.create(service.createProduct(product))
                .expectError(DuplicateProductException.class)
                .verify();
    }
}