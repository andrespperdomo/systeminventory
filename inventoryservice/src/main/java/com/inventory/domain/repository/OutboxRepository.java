package com.inventory.domain.repository;

import java.util.List;

import com.inventory.domain.outbox.OutboxEvent;

public interface OutboxRepository {

    OutboxEvent save(OutboxEvent outboxEvent);

    List<OutboxEvent> findPending();

    void markAsSent(Long id);

    void incrementRetries(Long id);
}