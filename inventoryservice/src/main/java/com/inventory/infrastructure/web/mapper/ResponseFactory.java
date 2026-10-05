package com.inventory.infrastructure.web.mapper;

import com.inventory.infrastructure.web.response.HistoricalInventoryAttributes;
import com.inventory.infrastructure.web.response.HistoricalInventoryResponse;
import com.inventory.infrastructure.web.response.InventoryAttributes;
import com.inventory.infrastructure.web.response.InventoryResponse;

public final class ResponseFactory {

    private ResponseFactory() {}

    public static InventoryResponse inventory(
            String id,
            InventoryAttributes attributes) {

        return new InventoryResponse(
            new InventoryResponse.Data(
                "Inventory",
                id,
                attributes
            )
        );
    }

    public static HistoricalInventoryResponse historical(
            String id,
            HistoricalInventoryAttributes attributes) {

        return new HistoricalInventoryResponse(
            new HistoricalInventoryResponse.Data(
                "InventoryHistorical",
                id,
                attributes
            )
        );
    }
}
