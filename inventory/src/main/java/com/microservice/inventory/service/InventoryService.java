package com.microservice.inventory.service;


import com.microservice.inventory.exception.InsufficientStockException;
import com.microservice.inventory.exception.InventoryNotFoundException;
import com.microservice.inventory.model.Inventory;
import com.microservice.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository repository;

    @Transactional
    public Mono<Inventory> createInventory(Inventory inventory) {
        return Mono.fromCallable(() -> {
                    if (repository.existsByProductId(inventory.getProductId())) {
                        throw new IllegalArgumentException("Inventory already exists for product: " + inventory.getProductId());
                    }
                    return repository.save(inventory);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional(readOnly = true)
    public Mono<Inventory> getInventoryByProductId(UUID productId) {
        return Mono.fromCallable(() -> repository.findByProductId(productId)
                        .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<Inventory> updateStock(UUID productId, int quantityDelta) {
        return Mono.fromCallable(() -> {
                    Inventory inventory = repository.findByProductId(productId)
                            .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product: " + productId));

                    int newQuantity = inventory.getQuantity() + quantityDelta;
                    if (newQuantity < 0) {
                        throw new InsufficientStockException("Insufficient stock. Current quantity: " + inventory.getQuantity());
                    }

                    inventory.setQuantity(newQuantity);
                    return repository.save(inventory); // Usa @Version para optimistic locking
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional(readOnly = true)
    public Flux<Inventory> getAllInventory() {
        return Flux.fromIterable(repository.findAll())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
