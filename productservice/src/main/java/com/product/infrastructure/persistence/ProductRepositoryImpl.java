package com.product.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import com.product.domain.model.Product;
import com.product.domain.model.ProductEntityMapper;
import com.product.domain.repository.ProductRepository;
import com.product.shared.utils.PageResult;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProductRepositoryImpl implements ProductRepository  {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public Product save(Product product) {

        ProductEntity entity = ProductEntityMapper.toEntity(product);

        if (entity.getId() == null) {
            em.persist(entity);
            em.flush();
        } else {
            entity = em.merge(entity);
        }

        return ProductEntityMapper.toDomain(entity);
    }

    @Override
    public Optional<Product> findById(Long id) {

        ProductEntity entity = em.find(ProductEntity.class, id);

        return Optional.ofNullable(entity)
                .map(ProductEntityMapper::toDomain);
    }

    @Override
   public boolean findByName(String name) {

    return !em.createQuery(
        "SELECT p FROM ProductEntity p WHERE p.name = :name",
        ProductEntity.class
    )
    .setParameter("name", name)
    .setMaxResults(1).getResultList()
    .isEmpty();
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

    public PageResult<Product> findAllPage(String search, int page, int size) {

        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);

        boolean hasSearch = hasSearch(search);

        String whereClause = hasSearch ? " WHERE LOWER(p.name) LIKE LOWER(:search)" : "";
        String orderBy = " ORDER BY p.id";

        // 🔹 Data query
        var query = em.createQuery(
                "FROM ProductEntity p" + whereClause + orderBy,
                ProductEntity.class);

        setSearchParameter(query, search, hasSearch);

        List<ProductEntity> entities = query
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        // 🔹 Count query
        var countQuery = em.createQuery(
                "SELECT COUNT(p) FROM ProductEntity p" + whereClause,
                Long.class);

        setSearchParameter(countQuery, search, hasSearch);

        long total = countQuery.getSingleResult();

        List<Product> products = entities.stream()
                .map(ProductEntityMapper::toDomain)
                .toList();

        return new PageResult<>(products, total, page, size);
    }

    private boolean hasSearch(String search) {
        return search != null && !search.isBlank();
    }

    private void setSearchParameter(jakarta.persistence.Query query, String search, boolean hasSearch) {
        if (hasSearch) {
            query.setParameter("search", "%" + search + "%");
        }
    }

}
