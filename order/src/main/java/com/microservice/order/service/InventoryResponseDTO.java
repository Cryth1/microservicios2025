package com.microservice.order.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponseDTO {
    private UUID productId;
    private Integer quantity;
    private String location;
}