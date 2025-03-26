package com.microservice.apigateway.circuitbreaker;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @RequestMapping("/orderFallback")
    public ResponseEntity<String> orderFallback() {
        return ResponseEntity.status(503).body("Order Service no disponible. Intente más tarde.");
    }

    @RequestMapping("/inventoryFallback")
    public ResponseEntity<String> inventoryFallback() {
        return ResponseEntity.status(503).body("Inventory Service no disponible.");
    }

}