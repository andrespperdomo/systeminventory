package com.inventory.domain.model;

import com.inventory.domain.outbox.OutboxEvent;
import com.inventory.infrastructure.persistence.OutboxEventEntity;

public class OutboxEntityMapper {

    public static OutboxEventEntity toEntity(OutboxEvent outbox) {
        OutboxEventEntity entity = new OutboxEventEntity();
        // entity.setId(product.id);
        entity.aggregateId = outbox.aggregateId();
        entity.payload = outbox.payload();
        entity.status = outbox.status();
        entity.type = outbox.type();
        return entity;
    }

    public static OutboxEvent toDomain(OutboxEventEntity entity) {
        return new OutboxEvent(
                entity.id,
                entity.aggregateId,
                entity.type,
                entity.payload,
                entity.status, entity.retries);
    }
}