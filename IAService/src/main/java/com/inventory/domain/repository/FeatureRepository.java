package com.inventory.domain.repository;

import java.util.Optional;

import com.inventory.domain.model.AIProductFeature;

public interface FeatureRepository {

    Optional<AIProductFeature> findByProductId(Long id);

    AIProductFeature save(AIProductFeature feature);

}