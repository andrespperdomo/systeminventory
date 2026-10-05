package com.inventory.infrastructure.web.response;

public record Meta(
        long total,
        int page,
        int size) {

}
