package com.inventory.infrastructure.web.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

public class PurchaseRequest {

    @Schema(description = "IdProduct", example = "1")
    @NotBlank(message = "IdProduct is required")
    public String idProduct;

    @Schema(description = "IdUser", example = "1")
    @NotBlank(message = "IdUser is required")
    public String idUser;

    @Schema(description = "reserved", example = "20")
    public Integer reserved;

}
