/*package com.inventory.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(RedisTestResource.class)
class RedisReservationRepositoryTest {

    @Inject
    RedisReservationRepository repository;

    @Test
    void shouldSaveAndRetrieveReservation() {

        String key = "reservationmeta:100:test";

        String payload = """
        {
          "productId":100,
          "userId":"user1",
          "reserved":2
        }
        """;

        repository.save(
                key,
                "data",
                payload,
                Duration.ofMinutes(5));

        Optional<String> value =
                repository.find(key, "data", String.class);

        assertTrue(value.isPresent());
        assertTrue(value.get().contains("\"productId\":100"));
        assertTrue(value.get().contains("\"reserved\":2"));
    }
}*/