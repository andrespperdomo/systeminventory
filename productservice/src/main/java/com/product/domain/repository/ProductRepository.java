package com.product.domain.repository;

import java.util.Optional;

import com.product.domain.model.Product;
import com.product.shared.utils.PageResult;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    public boolean findByName(String name);

    public PageResult<Product> findAllPage(String search, int page, int size);

}
