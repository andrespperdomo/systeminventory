package com.inventory.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.inventory.domain.model.HistoricalPurchaseMapper;
import com.inventory.domain.model.PurchaseHistory;
import com.inventory.domain.repository.HistoricalPurchaseRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class HistoricalPurchaseRepositoryImpl implements HistoricalPurchaseRepository {

    private static final Logger LOG = Logger.getLogger(HistoricalPurchaseRepositoryImpl.class);

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public PurchaseHistory save(PurchaseHistory purchaseHistory) {
        LOG.infof("HistoricalPurchaseRepositoryImpl | save purchaseHistory.productId=%s", purchaseHistory.productId());
        PurchaseHistoryEntity entity = new PurchaseHistoryEntity();
        entity.setReservationId(purchaseHistory.reservationId());
        entity.setProductId(purchaseHistory.productId());
        entity.setQuantity(purchaseHistory.quantity());
        entity.setType(purchaseHistory.type());
        entity.setIdUser(purchaseHistory.idUser());
        entity.setCreatedAt(LocalDateTime.now());
        em.persist(entity);
        LOG.infof("HistoricalPurchaseRepositoryImpl | purchaseHistory saved with id=%s", entity.id);
        return HistoricalPurchaseMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public PurchaseHistory update(PurchaseHistory purchaseHistory){
        LOG.infof("HistoricalPurchaseRepositoryImpl | update purchaseHistory.productId=%s", purchaseHistory.productId());
        PurchaseHistoryEntity entity = em.createQuery(
                "SELECT i FROM PurchaseHistoryEntity i WHERE i.reservationId = :reservationId",
                PurchaseHistoryEntity.class)
                .setParameter("reservationId", purchaseHistory.reservationId())
                .getResultStream()
                .findFirst()
                .orElse(null);

                entity.setType(purchaseHistory.type());
                em.merge(entity);
                 LOG.infof("HistoricalPurchaseRepositoryImpl | purchaseHistory saved with id=%s", entity.id);
                  return HistoricalPurchaseMapper.toDomain(entity);

    }

    @Override
    public Optional<PurchaseHistory> findById(Long id) {
        PurchaseHistoryEntity entity = em.createQuery(
                "SELECT i FROM PurchaseHistoryEntity i WHERE i.id = :id",
                PurchaseHistoryEntity.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);

        return Optional.ofNullable(entity)
                .map(HistoricalPurchaseMapper::toDomain);
    }

}
