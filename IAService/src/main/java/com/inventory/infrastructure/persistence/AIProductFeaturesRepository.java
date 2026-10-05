package com.inventory.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AIProductFeaturesRepository
        implements PanacheRepositoryBase<AIProductFeaturesEntity, Long> {
}
