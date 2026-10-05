package com.product.infrastructure.security.dto;

public record AuthResponse(
        String token,
        String bearer,
        String username,
        String role

) {
}