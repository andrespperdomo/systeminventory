package com.inventory.application.service;

import org.jboss.logging.Logger;

import com.inventory.application.messaging.EventEnvelope;
import com.inventory.application.usecase.ConfirmPurchaseUseCase;
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

     private static final Logger LOG = Logger.getLogger(OutboxEventPublisher.class);

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

      try {

    String json = jsonSerializer.toJson(envelope);

    LOG.infof(
            "Reservation event JSON: %s",
            json
    );

    OutboxEvent outboxEvent = new OutboxEvent(
            Long.parseLong(aggregateId),
            aggregateId,
            type,
            json,
            Status.PENDING.name(),
            0
    );

    outboxRepository.save(outboxEvent);

} catch (Exception e) {

    LOG.error(
            "Error converting event to JSON",
            e
    );

    throw e;
}
    }
}
