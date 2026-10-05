package com.inventory.application.service;

import com.inventory.application.messaging.EventEnvelope;
import com.inventory.domain.model.Status;
import com.inventory.domain.outbox.OutboxEvent;
import com.inventory.domain.repository.OutboxRepository;
import com.inventory.shared.utils.JsonUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final JsonUtil jsonSerializer;

    @Inject
    public OutboxEventPublisher(OutboxRepository outboxRepository,
            JsonUtil jsonSerializer) {
        this.outboxRepository = outboxRepository;
        this.jsonSerializer = jsonSerializer;
    }

    public <T> void publish(
            String aggregateId,
            String type,
            T event) {
        EventEnvelope<T> envelope = new EventEnvelope<>(type, event);

        outboxRepository.save(new OutboxEvent(
                Long.parseLong(aggregateId),
                aggregateId,
                type,
                jsonSerializer.toJson(envelope),
                Status.PENDING.name(),
                0));
    }
}
