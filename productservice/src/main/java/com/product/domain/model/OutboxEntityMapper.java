package com.product.domain.model;

import com.product.infrastructure.persistence.OutboxEventEntity;
import com.product.infrastructure.rabbitmq.model.OutboxEvent;

public class OutboxEntityMapper {

    public static OutboxEventEntity toEntity(OutboxEvent outbox) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.id = outbox.id();
        entity.aggregateId = outbox.aggregateId();
        entity.payload = outbox.payload();
        entity.status = outbox.status();
        entity.type = outbox.type();
        entity.retries = outbox.retries();
        return entity;
    }

    public static OutboxEvent toDomain(OutboxEventEntity entity) {
        return new OutboxEvent(entity.id,
                entity.aggregateId,
                entity.type,
                entity.payload,
                entity.status, entity.retries);
    }
}