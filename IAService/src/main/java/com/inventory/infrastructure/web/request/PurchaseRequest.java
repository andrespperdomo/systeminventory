package com.inventory.infrastructure.web.request;

import java.math.BigDecimal;

public record PurchaseRequest(
        int quantity,
        BigDecimal price
) {
}