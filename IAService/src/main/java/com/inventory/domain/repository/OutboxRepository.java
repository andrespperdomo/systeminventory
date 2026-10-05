package com.inventory.domain.repository;

import java.util.List;

import com.inventory.domain.outbox.OutboxEvent;
import com.inventory.infrastructure.persistence.OutboxEventEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

public interface OutboxRepository extends PanacheRepository<OutboxEventEntity> {
    OutboxEvent save(OutboxEvent event);

    public List<OutboxEvent> findPending();
}
