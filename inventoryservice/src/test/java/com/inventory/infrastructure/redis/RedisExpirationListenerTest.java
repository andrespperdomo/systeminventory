package com.inventory.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;

class RedisExpirationListenerTest {

    @Test
    void shouldBeStartedEagerly() {
        assertTrue(RedisExpirationListener.class.isAnnotationPresent(Startup.class));
        assertTrue(RedisExpirationListener.class.isAnnotationPresent(Singleton.class));
    }

    @Test
    void shouldParseReservationExpiredKeys() {
        assertTrue(RedisExpirationListener.parseInventoryFromExpiredKey("reservationmeta:4").isPresent());
        assertFalse(RedisExpirationListener.parseInventoryFromExpiredKey("invalid").isPresent());
    }

    @Test
    void shouldParseJsonInventoryPayload() {
        var inventory = RedisExpirationListener.parseInventoryFromExpiredKey(
                "{\"idProduct\":\"4\",\"available\":10,\"reserved\":2,\"quantity\":12}");

        assertTrue(inventory.isPresent());
        assertEquals("4", inventory.get().idProduct());
        assertEquals(10, inventory.get().available());
        assertEquals(2, inventory.get().reserved());
    }

   /*  @Test
    void shouldParseReservedQuantityFromPayload() {
        assertEquals(2, RedisExpirationListener.parseReservedQuantity("{\"userId\":\"123\",\"reserved\":2}"));
        assertEquals(0, RedisExpirationListener.parseReservedQuantity(null));
    }*/
}
