package com.microservice.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductDTO {
    private UUID productId;
    private String name;
    private BigDecimal price;
}