package com.inventory.application.command;

import java.math.BigDecimal;

public record InventoryProductCommand(
   String productId,
   BigDecimal unitPrice

) {
    
}
