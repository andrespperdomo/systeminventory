package com.inventory.application.command;

public record SaveHistoricalPurchaseCommand(
     String idReservation,
     String idProduct,
                Integer quantity,
                String type, // PURCHASE | RESTOCK | RESERVE
                String idUser

) {

}
