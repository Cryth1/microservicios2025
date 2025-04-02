package com.microservice.inventory.controller;

import com.microservice.inventory.exception.InsufficientStockException;
import com.microservice.inventory.exception.InventoryNotFoundException;
import com.microservice.inventory.model.Inventory;
import com.microservice.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor

public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Inventory> createInventory(@RequestBody Inventory inventory) {
        return inventoryService.createInventory(inventory);
    }

    @GetMapping("/product/{productId}")
    public Mono<Inventory> getInventoryByProductId(@PathVariable UUID productId) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @GetMapping("/slow")
    public String slowEndpoint() throws InterruptedException {
        Thread.sleep(3000); // Simula un retraso
        return "OK";
    }

    @GetMapping("/error")
    public String errorEndpoint() {
        throw new RuntimeException("Fallo simulado");
    }

    @PatchMapping("/product/{productId}/stock")
    public Mono<Inventory> updateStock(
            @PathVariable UUID productId,
            @RequestParam("action") String action,
            @RequestParam("quantity") int quantity
    ) {
        if (!action.equalsIgnoreCase("add") && !action.equalsIgnoreCase("subtract")) {
            return Mono.error(new IllegalArgumentException("Invalid action. Use 'add' or 'subtract'"));
        }

        int delta = action.equalsIgnoreCase("add") ? quantity : -quantity;
        return inventoryService.updateStock(productId, delta);
    }

    @GetMapping
    public Flux<Inventory> getAllInventory() {
        return inventoryService.getAllInventory();
    }

    // Manejo de excepciones
    @ExceptionHandler(InventoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<String> handleInventoryNotFound(InventoryNotFoundException ex) {
        return Mono.just(ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<String> handleInsufficientStock(InsufficientStockException ex) {
        return Mono.just(ex.getMessage());
    }
}
