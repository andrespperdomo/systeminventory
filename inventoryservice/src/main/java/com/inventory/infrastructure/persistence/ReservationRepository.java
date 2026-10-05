package com.inventory.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ReservationRepository
        implements PanacheRepository<ReservationEntity> {

    public Optional<ReservationEntity> findByReservationId(
            String reservationId) {

        return find("reservationId", reservationId)
                .firstResultOptional();
    }

    public List<ReservationEntity> findActiveByProductId(
            String productId) {

        return find(
                "productId = ?1 and status = ?2",
                productId,
                ReservationStatus.ACTIVE).list();
    }

    public List<ReservationEntity> findExpiredReservations(
            LocalDateTime now) {

        return find(
                "status = ?1 and expiresAt <= ?2",
                ReservationStatus.ACTIVE,
                now).list();
    }

    public List<ReservationEntity> findByUserId(String userId) {

        return find("userId", userId).list();
    }
}
