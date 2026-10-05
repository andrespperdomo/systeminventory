package com.inventory.application.usecase;

import org.jboss.logging.Logger;

import com.inventory.domain.model.AIProductFeature;
import com.inventory.domain.repository.FeatureRepository;
import com.inventory.infrastructure.rabbitmq.event.ReservationExpiredEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ReservationExpiredUseCase {

    private static final Logger LOG =
            Logger.getLogger(ReservationExpiredUseCase.class);

    @Inject
    FeatureRepository featureRepository;

    @Transactional
    public void execute(ReservationExpiredEvent event) {

        LOG.infof(
                "Processing expired reservation for product: %s",
                event.idProduct()
        );

        Long productId = Long.parseLong(event.idProduct());

        AIProductFeature feature =
                featureRepository
                        .findByProductId(productId)
                        .orElse(new AIProductFeature(productId));

        feature.registerExpiration(
                event.reserved()
        );

        featureRepository.save(feature);

        LOG.infof(
                "Expiration processed successfully for product: %s",
                productId
        );
    }
}