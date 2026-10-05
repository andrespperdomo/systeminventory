package com.inventory.application.usecase;

import org.jboss.logging.Logger;

import com.inventory.domain.model.Inventory;
import com.inventory.domain.model.InventoryConfirm;
import com.inventory.domain.repository.InventoryRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReleaseReservationUseCase {

    private static final Logger LOG =
            Logger.getLogger(ReleaseReservationUseCase.class);

    @Inject
    InventoryRepository inventoryRepository;

    public void execute(
            String expiredKey,
            InventoryConfirm reservation) {

        Long productId =
                Long.valueOf(
                        expiredKey
                                .replace("reservation:", "")
                                .split("\\|")[0]);

        Inventory inventory =
                inventoryRepository
                        .findById(productId)
                        .orElseThrow();
       System.out.println("############################################################ base reserved DB "+inventory.reserved()+"///"+reservation.reserved());
        Inventory updated = inventory.releaseReservation(
                reservation.reserved());

        inventoryRepository.update(updated);

        LOG.infof(
                "Reservation released for product %s",
                productId);

    }

}