package com.inventory.infrastructure.web.request;



import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

public record ConfirmProductRequest(

    
    @Schema(description = "idReservation", examples = "100000")
    @NotBlank(message = "idReservation is required")
    String idReservation,

    @Schema(description = "idProduct", examples = "1")
    @NotBlank(message = "idProduct is required")
    String idProduct,

    @Schema(description = "idUser", examples = "50")
    @NotBlank(message = "idUser is required")
    String idUser

) {}

