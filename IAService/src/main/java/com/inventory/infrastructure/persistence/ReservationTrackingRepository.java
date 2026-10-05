package com.inventory.infrastructure.persistence;


import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class ReservationTrackingRepository
        implements PanacheRepository<ReservationTrackingEntity> {

    public Optional<ReservationTrackingEntity> findByReservationId(
            String reservationId) {

        return find("reservationId", reservationId)
                .firstResultOptional();
    }
}