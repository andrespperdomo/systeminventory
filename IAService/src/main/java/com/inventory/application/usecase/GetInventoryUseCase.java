package com.inventory.application.usecase;

import java.util.Optional;

import org.jboss.logging.Logger;

import com.inventory.domain.model.Inventory;
import com.inventory.domain.repository.InventoryRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetInventoryUseCase {

    private static final Logger LOG = Logger.getLogger(UpdateInventoryUseCase.class);

    private final InventoryRepository repository;

    @Inject
    public GetInventoryUseCase(InventoryRepository productRepository) {
        this.repository = productRepository;
    }

    public Optional<Inventory> execute(String id) {
        LOG.infof("Inventory getId | productId=%s ", id);
        return repository.findById(Long.parseLong(id));
    }

}
