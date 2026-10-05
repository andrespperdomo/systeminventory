package com.inventory.application.usecase;

import com.inventory.domain.model.AIProductFeature;
import com.inventory.domain.repository.FeatureRepository;
import com.inventory.infrastructure.rabbitmq.event.ReservationEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReservationCreatedUseCase {

   @Inject 
   private  FeatureRepository repository;

    public void execute(ReservationEvent event){
        Long productId=Long.parseLong(event.productId());
        AIProductFeature feature = repository

                .findByProductId(productId)

                .orElse(new AIProductFeature(productId));

        feature.registerReservation(event.quantity(), event.reservedAt());
        repository.save(feature);

    }

}