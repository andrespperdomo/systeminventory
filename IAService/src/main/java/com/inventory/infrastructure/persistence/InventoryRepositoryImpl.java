package com.inventory.infrastructure.persistence;

import java.util.Optional;

import org.jboss.logging.Logger;
import com.inventory.domain.model.Inventory;
import com.inventory.domain.model.InventoryEntityMapper;
import com.inventory.domain.repository.InventoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class InventoryRepositoryImpl implements InventoryRepository {

    private static final Logger LOG = Logger.getLogger(InventoryRepositoryImpl.class);

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public Inventory update(Inventory inventory) {
        LOG.infof("InventoryRepositoryImpl | inventory.productId=%s", inventory.idProduct());
        InventoryEntity entity = em.createQuery(
                "SELECT i FROM InventoryEntity i WHERE i.productId = :id",
                InventoryEntity.class)
                .setParameter("id", inventory.idProduct())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (entity != null) {
            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX????????? QUANTITY " + inventory.quantity());
            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX PRODUCT " + inventory.idProduct());
            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX AVAILABLE " + inventory.available());
            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX RESERVED " + inventory.reserved());
            System.out.println(em.contains(entity));

            LOG.infof(
                    "InventoryRepositoryImpl | inventory.productId=%s=================================================================",
                    inventory.quantity());
            entity.setQuantity(inventory.quantity());
            entity.setAvailableQuantity(inventory.available());
            entity.setReserved(inventory.reserved());
            em.merge(entity);
            em.flush();
            // entity.setAvailableQuantity(inventory.quantity());

        } else {
            System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX INSERTTTTTTT ");
            entity = new InventoryEntity();
            entity.setProductId(inventory.idProduct());
            entity.setQuantity(inventory.quantity());
            entity.setAvailableQuantity(inventory.quantity());
            em.persist(entity);
        }

        return InventoryEntityMapper.toDomain(entity);
    }

    @Override
    public Optional<Inventory> findById(Long id) {

        // InventoryEntity entity = em.find(InventoryEntity.class, id);

        InventoryEntity entity = em.createQuery(
                "SELECT i FROM InventoryEntity i WHERE i.productId = :id",
                InventoryEntity.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);        

        return Optional.ofNullable(entity)
                .map(InventoryEntityMapper::toDomain);
    }

    /*
     * @Override
     * public List<Product> findAll() {
     * 
     * List<ProductEntity> entities = em
     * .createQuery("FROM ProductEntity", ProductEntity.class)
     * .getResultList();
     * 
     * return entities.stream()
     * .map(ProductEntityMapper::toDomain)
     * .collect(Collectors.toList());
     * }
     */

    /*
     * public PageResult<Product> findAllPage(String search, int page, int size) {
     * 
     * page = Math.max(page, 0);
     * size = Math.min(Math.max(size, 1), 100);
     * 
     * boolean hasSearch = hasSearch(search);
     * 
     * String whereClause = hasSearch ? " WHERE LOWER(p.name) LIKE LOWER(:search)" :
     * "";
     * String orderBy = " ORDER BY p.id";
     * 
     * // 🔹 Data query
     * var query = em.createQuery(
     * "FROM ProductEntity p" + whereClause + orderBy,
     * ProductEntity.class);
     * 
     * setSearchParameter(query, search, hasSearch);
     * 
     * List<ProductEntity> entities = query
     * .setFirstResult(page * size)
     * .setMaxResults(size)
     * .getResultList();
     * 
     * // 🔹 Count query
     * var countQuery = em.createQuery(
     * "SELECT COUNT(p) FROM ProductEntity p" + whereClause,
     * Long.class);
     * 
     * setSearchParameter(countQuery, search, hasSearch);
     * 
     * long total = countQuery.getSingleResult();
     * 
     * List<Product> products = entities.stream()
     * .map(InventoryEntityMapper::toDomain)
     * .toList();
     * 
     * return new PageResult<>(products, total, page, size);
     * }
     * 
     * private boolean hasSearch(String search) {
     * return search != null && !search.isBlank();
     * }
     * 
     * private void setSearchParameter(jakarta.persistence.Query query, String
     * search, boolean hasSearch) {
     * if (hasSearch) {
     * query.setParameter("search", "%" + search + "%");
     * }
     * }
     */

}
