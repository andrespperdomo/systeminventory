package com.inventory.infrastructure.web.request;

public record SaveHistoricalPurchaseRequest(
       String idProduct,
                Integer quantity,
                String type, // PURCHASE | RESTOCK | RESERVE
                String idUser

) {
    
}
