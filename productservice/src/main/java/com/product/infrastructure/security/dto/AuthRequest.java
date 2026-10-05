package com.product.infrastructure.security.dto;


public record AuthRequest(
        String username,
        String password
) {
}