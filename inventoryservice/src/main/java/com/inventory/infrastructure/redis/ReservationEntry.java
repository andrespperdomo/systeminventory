package com.inventory.infrastructure.redis;

import java.util.Map;

public record ReservationEntry(
        String redisKey,
        Map<String, Object> data
) {}
