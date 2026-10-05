package com.inventory.infrastructure.persistence;

import java.util.List;

import com.inventory.domain.model.OutboxEntityMapper;
import com.inventory.domain.outbox.OutboxEvent;
import com.inventory.domain.repository.OutboxRepository;
import com.inventory.infrastructure.rabbitmq.enums.Status;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OutboxRepositoryImpl implements OutboxRepository {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent outboxEvent) {

        OutboxEventEntity entity = OutboxEntityMapper.toEntity(outboxEvent);

        if (entity.id == null) {
            em.persist(entity);
        } else {
            entity = em.merge(entity);
        }

        return OutboxEntityMapper.toDomain(entity);
    }

    public List<OutboxEvent> findPending() {

        List<OutboxEventEntity> entities = find(
                "status = ?1 and retries < ?2",
                Status.NEW.name(),
                3).list();

        return entities.stream()
                .map(event -> OutboxEntityMapper.toDomain(event))
                .toList();
    }

}
