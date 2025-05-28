package com.microservice.inventory.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponseDTO {
    private UUID productId;
    private Integer quantity;
    private String location;
}