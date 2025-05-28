package com.microservice.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;


@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Añadir Builder para facilitar la creación
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID productId; // Cambiado de 'product' a 'productId' por claridad
    private Integer quantity;
    private String customerEmail; // Considera un UUID de cliente si tienes un servicio de usuarios

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private java.math.BigDecimal totalAmount; // Es bueno guardar el monto calculado
}
