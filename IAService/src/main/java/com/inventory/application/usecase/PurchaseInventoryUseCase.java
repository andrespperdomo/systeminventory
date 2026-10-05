package com.inventory.application.usecase;

import java.time.Duration;
import java.time.LocalDateTime;

import com.inventory.domain.model.AIProductFeature;
import com.inventory.domain.repository.FeatureRepository;
import com.inventory.infrastructure.persistence.ReservationTrackingEntity;
import com.inventory.infrastructure.persistence.ReservationTrackingRepository;
import com.inventory.infrastructure.rabbitmq.event.InventoryConfirmEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PurchaseInventoryUseCase {

    @Inject
    FeatureRepository featureRepository;

    @Inject
    ReservationTrackingRepository reservationRepository;

    @Transactional
    public void execute(InventoryConfirmEvent event) {

        Long productId = Long.parseLong(event.productId());

        AIProductFeature feature = featureRepository
                .findByProductId(productId)
                .orElse(new AIProductFeature(productId));

        int quantity = event.quantity().intValue();

        LocalDateTime purchaseTime= LocalDateTime.parse(event.purchasedAt());

        ReservationTrackingEntity reservation =
                reservationRepository
                        .findByReservationId(event.reservationId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Reservation not found: "
                                                + event.reservationId()
                                )
                        );

        long purchaseTimeMinutes = Duration.between(
                reservation.getReservedAt(),
                purchaseTime
        ).toMinutes();

        feature.registerPurchase(
                quantity,
                event.unitPrice(),
                purchaseTime,
                purchaseTimeMinutes
        );

        featureRepository.save(feature);
    }
}