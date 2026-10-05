package com.inventory.infrastructure.redis;

public record RedisRecord(
        String key,
        String type,
        Object value) {
}