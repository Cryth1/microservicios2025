package com.microservice.order.model;

public enum OrderStatus {
    PENDING_VALIDATION, // Orden creada, esperando validación de producto y stock
    AWAITING_PAYMENT,   // Stock reservado, esperando pago
    CONFIRMED,          // Pago exitoso, orden confirmada
    SHIPPED,            // Orden enviada
    DELIVERED,          // Orden entregada
    CANCELLED_PRODUCT_NOT_FOUND, // Producto no encontrado
    CANCELLED_NO_STOCK, // No hay stock suficiente
    CANCELLED_PAYMENT_FAILED, // Fallo en el pago
    CANCELLED_BY_USER   // Cancelada por el usuario
}
