package com.inventory.infrastructure.web.response;

public record InventoryResponse(
    Data data
) {
    public record Data(
        String type,
        String id,
        InventoryAttributes attributes
    ) {}
}