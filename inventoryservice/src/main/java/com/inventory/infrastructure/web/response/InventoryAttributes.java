package com.inventory.infrastructure.web.response;

public record InventoryAttributes(
  
         String idProduct,
         Integer reserved,
         Integer available,
         Integer quantity

) {  
}
