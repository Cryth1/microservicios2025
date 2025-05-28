package com.microservice.order.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class InventoryUpdateResponseDTO { // Asumimos que InventoryService devuelve algo así
    private UUID productId;
    private Integer newQuantity;
    private String message; // Opcional, para errores o éxito
}