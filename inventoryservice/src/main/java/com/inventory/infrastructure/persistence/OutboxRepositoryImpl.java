package com.inventory.infrastructure.persistence;

import java.util.List;

import com.inventory.domain.model.OutboxEntityMapper;
import com.inventory.domain.outbox.OutboxEvent;

import com.inventory.domain.repository.OutboxRepository;
import com.inventory.infrastructure.rabbitmq.enums.Status;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import com.inventory.infrastructure.persistence.OutboxEventEntity;

@ApplicationScoped
public class OutboxRepositoryImpl
        implements OutboxRepository,
                   PanacheRepositoryBase<OutboxEventEntity, Long> {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent outboxEvent) {

        OutboxEventEntity entity =
                OutboxEntityMapper.toEntity(outboxEvent);

        if (entity.id == null) {
            em.persist(entity);
        } else {
            entity = em.merge(entity);
        }

        return OutboxEntityMapper.toDomain(entity);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void markAsSent(Long id) {

        OutboxEventEntity entity = findById(id);

        if (entity == null) {
            throw new IllegalStateException(
                "Outbox event not found: " + id
            );
        }

        entity.setStatus(Status.SEND.name());

       // flush();
    }

    @Override
    public List<OutboxEvent> findPending() {

        List<OutboxEventEntity> entities =
            find(
                "status = ?1 and retries < ?2",
                Status.PENDING.name(),
                3
            ).list();

        return entities.stream()
                .map(OutboxEntityMapper::toDomain)
                .toList();
    }

    @Override
   @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void incrementRetries(Long id) {

        OutboxEventEntity entity = findById(id);

        if (entity == null) {
            throw new IllegalStateException(
                "Outbox event not found: " + id
            );
        }

        entity.setRetries(entity.getRetries() + 1);

        flush();
    }
}