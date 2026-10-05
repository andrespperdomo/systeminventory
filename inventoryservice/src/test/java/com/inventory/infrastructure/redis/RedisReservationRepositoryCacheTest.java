package com.inventory.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisReservationRepository cache tests")
class RedisReservationRepositoryCacheTest {

    @Mock
    private RedisDataSource redisDataSource;

    @Mock
    private HashCommands<String, String, String> hashCommands;

    @Mock
    private KeyCommands keyCommands;

    @Mock
    private ObjectMapper objectMapper;

    private RedisReservationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RedisReservationRepository();
        repository.redisDataSource = redisDataSource;
        repository.objectMapper = objectMapper;

        lenient().when(redisDataSource.hash(String.class, String.class, String.class)).thenReturn(hashCommands);
        lenient().when(redisDataSource.key()).thenReturn(keyCommands);
    }

    @Test
    @DisplayName("Should save a value in Redis and set TTL")
    void shouldSaveValueAndSetTtl() throws Exception {
        when(objectMapper.writeValueAsString("reserved-2")).thenReturn("\"reserved-2\"");

        repository.save("reservation:4", "data", "reserved-2", Duration.ofSeconds(150));

        verify(hashCommands).hset("reservation:4", "data", "\"reserved-2\"");
        verify(keyCommands).expire("reservation:4", Duration.ofSeconds(150));
    }

    @Test
    @DisplayName("Should read a saved value from Redis")
    void shouldReadSavedValueFromRedis() throws Exception {
        when(hashCommands.hget("reservation:4", "data")).thenReturn("\"reserved-2\"");
        when(objectMapper.readValue("\"reserved-2\"", String.class)).thenReturn("reserved-2");

        Optional<String> result = repository.find("reservation:4", "data", String.class);

        assertTrue(result.isPresent());
        assertEquals("reserved-2", result.get());
        verify(hashCommands).hget("reservation:4", "data");
    }

    @Test
    @DisplayName("Should delete a field from Redis")
    void shouldDeleteFieldFromRedis() {
        repository.delete("reservation:4", "data");

        verify(hashCommands).hdel("reservation:4", "data");
    }
}
