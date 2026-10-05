package com.inventory.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.domain.model.Inventory;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import io.quarkus.redis.datasource.keys.KeyCommands;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationRedisRepository Tests")
class ReservationRedisRepositoryTest {

    @Mock
    private RedisDataSource redisDataSource;

    @Mock
    private HashCommands<String, String, String> hashCommands;

    @Mock
    private KeyCommands keyCommands;

    @Mock
    private ObjectMapper objectMapper;

    private ReservationRedisRepository repository;

    private static final String PRODUCT_ID = "prod-123";
    private static final String USER_ID = "user-456";
    private static final Integer QUANTITY = 5;
    private static final String REDIS_KEY = "reservation:" + PRODUCT_ID;
    private static final long TTL_SECONDS = 900;

    @BeforeEach
    void setUp() throws Exception {
        repository = new ReservationRedisRepository(redisDataSource);
        repository.objectMapper = objectMapper;
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}\n");
        lenient().when(redisDataSource.hash(String.class, String.class, String.class)).thenReturn(hashCommands);
        lenient().when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
        lenient().when(redisDataSource.key()).thenReturn(keyCommands);
    }

    // =========================
    // RESERVE TESTS
    // =========================

    @Test
    @DisplayName("Should reserve successfully with valid parameters")
    void shouldReserveSuccessfully() {
        // Arrange
        when(redisDataSource.hash(String.class, String.class, String.class)).thenReturn(hashCommands);
        when(redisDataSource.key()).thenReturn(keyCommands);
        Inventory inventory = new Inventory(null, PRODUCT_ID, USER_ID, QUANTITY, 0, 0);

        // Act
        repository.reserve(inventory);

        // Assert
        verify(hashCommands).hset(eq(REDIS_KEY), eq(USER_ID), anyString());
        verify(keyCommands).expire(REDIS_KEY, TTL_SECONDS);
    }

    @Test
    @DisplayName("Should not reserve when productId is null")
    void shouldNotReserveWhenProductIdIsNull() {
        // Act
        repository.reserve(new Inventory(null, null, USER_ID, QUANTITY, 0, 0));

        // Assert
        verify(redisDataSource, never()).hash(String.class, String.class, String.class);
        verify(redisDataSource, never()).key();
    }

    @Test
    @DisplayName("Should not reserve when productId is blank")
    void shouldNotReserveWhenProductIdIsBlank() {
        // Act
        repository.reserve(new Inventory(null, "", USER_ID, QUANTITY, 0, 0));

        // Assert
        verify(redisDataSource, never()).hash(String.class, String.class, String.class);
        verify(redisDataSource, never()).key();
    }

    @Test
    @DisplayName("Should not reserve when userId is null")
    void shouldNotReserveWhenUserIdIsNull() {
        // Act
        repository.reserve(new Inventory(null, PRODUCT_ID, null, QUANTITY, 0, 0));

        // Assert
        verify(redisDataSource, never()).hash(String.class, String.class, String.class);
        verify(redisDataSource, never()).key();
    }

    @Test
    @DisplayName("Should not reserve when userId is blank")
    void shouldNotReserveWhenUserIdIsBlank() {
        // Act
        repository.reserve(new Inventory(null, PRODUCT_ID, "", QUANTITY, 0, 0));

        // Assert
        verify(redisDataSource, never()).hash(String.class, String.class, String.class);
        verify(redisDataSource, never()).key();
    }

    @Test
    @DisplayName("Should not reserve when quantity is null")
    void shouldNotReserveWhenQuantityIsNull() {
        // Act
        repository.reserve(new Inventory(null, PRODUCT_ID, USER_ID, null, 0, 0));

        // Assert
        verify(redisDataSource, never()).hash(String.class, String.class, String.class);
        verify(redisDataSource, never()).key();
    }

    @Test
    @DisplayName("Should not reserve when quantity is zero")
    void shouldNotReserveWhenQuantityIsZero() {
        // Act
        repository.reserve(new Inventory(null, PRODUCT_ID, USER_ID, 0, 0, 0));

        // Assert
        verify(redisDataSource, never()).hash(String.class, String.class, String.class);
        verify(redisDataSource, never()).key();
    }

    @Test
    @DisplayName("Should not reserve when quantity is negative")
    void shouldNotReserveWhenQuantityIsNegative() {
        // Act
        repository.reserve(new Inventory(null, PRODUCT_ID, USER_ID, -5, 0, 0));

        // Assert
        verify(redisDataSource, never()).hash(String.class, String.class, String.class);
        verify(redisDataSource, never()).key();
    }

    @Test
    @DisplayName("Should handle Redis exception during reserve")
    void shouldHandleRedisExceptionDuringReserve() {
        // Arrange
        when(redisDataSource.hash(String.class, String.class, String.class)).thenReturn(hashCommands);
        doThrow(new RuntimeException("Redis connection failed")).when(hashCommands).hset(any(), any(), any());
        Inventory inventory = new Inventory(null, PRODUCT_ID, USER_ID, QUANTITY, 0, 0);

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> repository.reserve(inventory));
        verify(hashCommands).hset(eq(REDIS_KEY), eq(USER_ID), anyString());
    }

    @Test
    @DisplayName("Should set correct TTL after reservation")
    void shouldSetCorrectTTLAfterReservation() {
        // Arrange
        when(redisDataSource.hash(String.class, String.class, String.class)).thenReturn(hashCommands);
        when(redisDataSource.key()).thenReturn(keyCommands);

        // Act
        repository.reserve(new Inventory(null, PRODUCT_ID, USER_ID, QUANTITY, 0, 0));

        // Assert
        verify(keyCommands).expire(REDIS_KEY, TTL_SECONDS);
    }

    // =========================
    // GET RESERVATION TESTS
    // =========================

    @Test
    @DisplayName("Should get reservation successfully when it exists")
    void shouldGetReservationSuccessfully() {
        // Arrange
        when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
        when(hashCommands.hget(REDIS_KEY, USER_ID)).thenReturn(String.valueOf(QUANTITY));

        // Act
        Optional<Integer> result = repository.getReservation(PRODUCT_ID, USER_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(QUANTITY, result.get());
        verify(hashCommands).hget(REDIS_KEY, USER_ID);
    }

    @Test
    @DisplayName("Should return empty when reservation does not exist")
    void shouldReturnEmptyWhenReservationDoesNotExist() {
        // Arrange
        when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
        when(hashCommands.hget(REDIS_KEY, USER_ID)).thenReturn(null);

        // Act
        Optional<Integer> result = repository.getReservation(PRODUCT_ID, USER_ID);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when reservation value is blank")
    void shouldReturnEmptyWhenReservationValueIsBlank() {
        // Arrange
        when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
        when(hashCommands.hget(REDIS_KEY, USER_ID)).thenReturn("");

        // Act
        Optional<Integer> result = repository.getReservation(PRODUCT_ID, USER_ID);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when productId is null")
    void shouldReturnEmptyWhenProductIdIsNull() {
        // Act
        Optional<Integer> result = repository.getReservation(null, USER_ID);

        // Assert
        assertTrue(result.isEmpty());
        verify(redisDataSource, never()).hash(String.class);
    }

    @Test
    @DisplayName("Should return empty when productId is blank")
    void shouldReturnEmptyWhenProductIdIsBlank() {
        // Act
        Optional<Integer> result = repository.getReservation("", USER_ID);

        // Assert
        assertTrue(result.isEmpty());
        verify(redisDataSource, never()).hash(String.class);
    }

    @Test
    @DisplayName("Should return empty when userId is null")
    void shouldReturnEmptyWhenUserIdIsNull() {
        // Act
        Optional<Integer> result = repository.getReservation(PRODUCT_ID, null);

        // Assert
        assertTrue(result.isEmpty());
        verify(redisDataSource, never()).hash(String.class);
    }

    @Test
    @DisplayName("Should return empty when userId is blank")
    void shouldReturnEmptyWhenUserIdIsBlank() {
        // Act
        Optional<Integer> result = repository.getReservation(PRODUCT_ID, "");

        // Assert
        assertTrue(result.isEmpty());
        verify(redisDataSource, never()).hash(String.class);
    }

    @Test
    @DisplayName("Should handle NumberFormatException when value is not an integer")
    void shouldHandleNumberFormatExceptionWhenValueIsNotInteger() {
        // Arrange
        when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
        when(hashCommands.hget(REDIS_KEY, USER_ID)).thenReturn("not-a-number");

        // Act
        Optional<Integer> result = repository.getReservation(PRODUCT_ID, USER_ID);

        // Assert
        assertTrue(result.isEmpty());
        verify(hashCommands).hget(REDIS_KEY, USER_ID);
    }

    @Test
    @DisplayName("Should handle Redis exception during get reservation")
    void shouldHandleRedisExceptionDuringGetReservation() {
        // Arrange
        when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
        when(hashCommands.hget(REDIS_KEY, USER_ID)).thenThrow(new RuntimeException("Redis connection failed"));

        // Act
        Optional<Integer> result = repository.getReservation(PRODUCT_ID, USER_ID);

        // Assert
        assertTrue(result.isEmpty());
        verify(hashCommands).hget(REDIS_KEY, USER_ID);
    }

    @Test
    @DisplayName("Should parse integer value correctly")
    void shouldParseIntegerValueCorrectly() {
        // Arrange
        Integer expectedQuantity = 123;
        when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
        when(hashCommands.hget(REDIS_KEY, USER_ID)).thenReturn(String.valueOf(expectedQuantity));

        // Act
        Optional<Integer> result = repository.getReservation(PRODUCT_ID, USER_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedQuantity, result.get());
    }

    // =========================
    // REMOVE TESTS
    // =========================

    @Test
    @DisplayName("Should remove reservation successfully with valid parameters")
    void shouldRemoveReservationSuccessfully() {
        // Arrange
        when(redisDataSource.hash(String.class)).thenReturn(hashCommands);

        // Act
        repository.remove(PRODUCT_ID, USER_ID);

        // Assert
        verify(hashCommands).hdel(REDIS_KEY, USER_ID);
    }

    @Test
    @DisplayName("Should not remove when productId is null")
    void shouldNotRemoveWhenProductIdIsNull() {
        // Act
        repository.remove(null, USER_ID);

        // Assert
        verify(redisDataSource, never()).hash(String.class);
    }

    @Test
    @DisplayName("Should not remove when productId is blank")
    void shouldNotRemoveWhenProductIdIsBlank() {
        // Act
        repository.remove("", USER_ID);

        // Assert
        verify(redisDataSource, never()).hash(String.class);
    }

    @Test
    @DisplayName("Should not remove when userId is null")
    void shouldNotRemoveWhenUserIdIsNull() {
        // Act
        repository.remove(PRODUCT_ID, null);

        // Assert
        verify(redisDataSource, never()).hash(String.class);
    }

    @Test
    @DisplayName("Should not remove when userId is blank")
    void shouldNotRemoveWhenUserIdIsBlank() {
        // Act
        repository.remove(PRODUCT_ID, "");

        // Assert
        verify(redisDataSource, never()).hash(String.class);
    }

    @Test
    @DisplayName("Should handle Redis exception during remove")
    void shouldHandleRedisExceptionDuringRemove() {
        // Arrange
        when(redisDataSource.hash(String.class)).thenReturn(hashCommands);
        doThrow(new RuntimeException("Redis connection failed")).when(hashCommands).hdel(any(), any());

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> repository.remove(PRODUCT_ID, USER_ID));
        verify(hashCommands).hdel(REDIS_KEY, USER_ID);
    }
}
