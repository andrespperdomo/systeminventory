
package com.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.inventory.domain.model.AIProductFeature;
import com.inventory.domain.repository.FeatureRepository;

@ApplicationScoped
@Transactional
public class PanacheFeatureRepository implements FeatureRepository {

    @Inject
    AIProductFeaturesRepository repository;

    @Override
    public Optional<AIProductFeature> findByProductId(Long id) {

        return repository.findByIdOptional(id)
                .map(entity -> new AIProductFeature(
                        entity.getProductId(),
                        entity.getReservations(),
                        entity.getPurchases(),
                        entity.getExpired(),
                        entity.getReservedQuantity(),
                        entity.getPurchasedQuantity(),
                        entity.getExpiredQuantity(),
                        entity.getRevenue(),
                        entity.getTotalPurchaseTimeMinutes() != null
                                ? entity.getTotalPurchaseTimeMinutes().longValue()
                                : 0L,
                        entity.getPurchaseTimeSamples() != null
                                ? entity.getPurchaseTimeSamples()
                                : 0,
                        entity.getLastPurchase(),
                        entity.getLastReservation()
                ));
    }

    @Override
    public AIProductFeature save(AIProductFeature feature) {

        Optional<AIProductFeaturesEntity> existing =
                repository.findByIdOptional(feature.getProductId());

        AIProductFeaturesEntity entity;

        if (existing.isPresent()) {
            // Existing entity is already managed by Hibernate
            entity = existing.get();
        } else {
            // New entity
            entity = new AIProductFeaturesEntity();
            entity.setProductId(feature.getProductId());
        }

        entity.setReservations(feature.getReservations());
        entity.setPurchases(feature.getPurchases());
        entity.setExpired(feature.getExpired());

        entity.setReservedQuantity(
                feature.getReservedQuantity()
        );

        entity.setPurchasedQuantity(
                feature.getPurchasedQuantity()
        );

        entity.setExpiredQuantity(
                feature.getExpiredQuantity()
        );

        entity.setRevenue(
                feature.getRevenue()
        );

        entity.setTotalPurchaseTimeMinutes(
                BigDecimal.valueOf(
                        feature.getTotalPurchaseTimeMinutes()
                )
        );

        entity.setPurchaseTimeSamples(
                feature.getPurchaseTimeSamples()
        );

        entity.setAverageReservedQuantity(
                feature.getAverageReservedQuantity()
        );

        entity.setAveragePurchaseTimeMinutes(
                feature.getAveragePurchaseTimeMinutes()
        );

        entity.setCancellationRate(
                feature.getCancellationRate()
        );

        entity.setConversionRate(
                feature.getConversionRate()
        );

        entity.setLastPurchase(
                feature.getLastPurchase()
        );

        entity.setLastReservation(
                feature.getLastReservation()
        );

        if (existing.isEmpty()) {
            repository.persist(entity);
        }

        return feature;
    }
}