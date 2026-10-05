package com.inventory.application.usecase;

import java.time.LocalDateTime;

import com.inventory.infrastructure.persistence.InventoryProductEntity;
import com.inventory.infrastructure.persistence.ReservationEntity;
import com.inventory.infrastructure.persistence.ReservationRepository;
import com.inventory.infrastructure.persistence.ReservationStatus;
import com.inventory.application.usecase.ReservationUseCase;
import com.inventory.application.command.ReservationCommand;
import com.inventory.infrastructure.persistence.InventoryProductRepository;
import com.inventory.infrastructure.persistence.InventoryProductEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;   
import java.util.UUID;

@ApplicationScoped
public class ReservationUseCase {

    @Inject
    InventoryProductRepository inventoryProductRepository;

    @Inject
    ReservationRepository reservationRepository;

    @Transactional
        public void execute(ReservationCommand command) {

            String productId = command.productId();
InventoryProductEntity product =
        inventoryProductRepository.findByProductId(productId)
                .orElseThrow();

ReservationEntity reservation = ReservationEntity.builder()
        .reservationId(command.reservationId())
        .productId(productId)
        .userId(command.userId())
        .quantity(command.quantity())
        .unitPrice(product.getUnitPrice()) // snapshot
        .reservedAt(LocalDateTime.now())
        .expiresAt(LocalDateTime.now().plusMinutes(15))
        .status(ReservationStatus.ACTIVE)
        .build();

reservationRepository.persist(reservation);
        }
    
}
