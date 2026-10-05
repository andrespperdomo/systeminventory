package com.inventory.application.usecase;

import com.inventory.application.command.ReservationTrackingCommand;
import com.inventory.infrastructure.persistence.ReservationTrackingEntity;
import com.inventory.infrastructure.persistence.ReservationTrackingRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class  ReservationTrackingUseCase {

    private final ReservationTrackingRepository repository;

    public ReservationTrackingUseCase(
            ReservationTrackingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(ReservationTrackingCommand command) {

        ReservationTrackingEntity tracking =
                new ReservationTrackingEntity(command.reservationId(),
                 command.productId(), 
                 command.quantity(), command.reservedAt());

        repository.persist(tracking);
    }
}