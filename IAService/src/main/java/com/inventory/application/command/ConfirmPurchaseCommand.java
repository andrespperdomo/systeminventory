package com.inventory.application.command;

public record ConfirmPurchaseCommand(
        String idReservation,
        String idProduct,
        String idUser) {

}
