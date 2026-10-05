package com.inventory.infrastructure.web.response;

public record HistoricalInventoryResponse(
    Data data
) {
    public record Data(
        String type,
        String id,
        HistoricalInventoryAttributes attributes
    ) {}
}