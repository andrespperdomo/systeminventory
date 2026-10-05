package com.inventory.shared.utils;

import com.inventory.shared.utils.constants.RedisKeyPrefix;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExpireRedisUtil {

    public String buildMetaKey(String expiredKey) {

        return expiredKey.replace(
                RedisKeyPrefix.RESERVATION.getValue(),
                RedisKeyPrefix.RESERVATION_META.getValue());
    }

    public Long extractProductId(String expiredKey) {

        return Long.valueOf(

                expiredKey

                        .replace(RedisKeyPrefix.RESERVATION.getValue(), "")

                        .split("\\|")[0]);
    }

    public String extractReservationId(String expiredKey) {

        return expiredKey.split("\\|")[1];
    }

}