package com.microservice.inventory.repository;

import com.microservice.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findByProductId(UUID productId);
    boolean existsByProductId(UUID productId);

    @Override
    <S extends Inventory> S saveAndFlush(S entity); // Habilita saveAndFlush
}