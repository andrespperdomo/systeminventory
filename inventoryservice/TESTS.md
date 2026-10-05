# Inventory Service Testing Documentation

## 1. Purpose

This document describes the current test strategy for the `inventoryservice` project, explains existing test coverage, and provides guidance for adding reliable tests at a senior level.

## 2. Test Frameworks and Tools

The service uses the following test technologies:

- JUnit Jupiter (`org.junit.jupiter:junit-jupiter`) for unit test execution and assertions
- Mockito (`org.mockito:mockito-core`, `org.mockito:mockito-junit-jupiter`) for object mocking and verification
- Quarkus is not currently used directly in tests, so the test suite is isolated from Quarkus runtime behavior

## 3. Current Test Coverage

There are currently two test classes in the project:

### 3.1 `InventoryControllerTest`

Location: `src/test/java/com/inventory/infrastructure/web/controller/InventoryControllerTest.java`

This suite validates the controller layer and ensures the REST adapter behaves correctly when invoking application use cases.

Key responsibilities covered:

- Mapping incoming web requests into command objects via `InventoryMapper`
- Returning correct HTTP status codes for success and failure cases
- Delegating to application use cases and converting domain results to response objects

Covered scenarios:

- Successful inventory update returns `201 Created`
- Successful `GET /inventory/{id}` returns `200 OK` with mapped entity payload
- `GET /inventory/{id}` when not found returns `404 Not Found`

Testing approach:

- Uses Mockito to mock `UpdateInventoryUseCase`, `GetInventoryUseCase`, and `InventoryMapper`
- Uses `@InjectMocks` to instantiate the controller under test
- Focuses on controller behavior rather than real persistence or business logic

### 3.2 `ReservationRedisRepositoryTest`

Location: `src/test/java/com/inventory/infrastructure/redis/ReservationRedisRepositoryTest.java`

This suite validates the Redis adapter implementation for reservation storage.

Key responsibilities covered:

- Persisting reservation metadata in Redis hashes
- Enforcing validation rules for inputs before writing to Redis
- Handling TTL expiration on reservation keys
- Reading reservation details and converting stored values
- Removing reservation entries from Redis
- Handling Redis runtime failures gracefully

Covered scenarios:

- `reserve()` successfully stores a reservation and sets TTL
- `reserve()` rejects invalid inputs such as null or blank product/user IDs, zero or negative quantity
- `reserve()` does not propagate exceptions on Redis failures
- `getReservation()` returns the stored integer quantity when present
- `getReservation()` returns empty for missing keys, blank values, invalid integers, or exceptions
- `remove()` deletes the reservation field for valid IDs
- `remove()` safely ignores invalid input and Redis failure conditions

Testing approach:

- Uses Mockito to mock `RedisDataSource`, `HashCommands`, `KeyCommands`, and `ObjectMapper`
- Verifies the repository adapter observes validation rules before interacting with Redis
- Ensures adapter-level exception handling preserves service stability

## 4. What Is Not Covered Today

The current test suite focuses on infrastructure adapters and web-layer request handling. The following areas are not covered by tests yet:

- Application use case business logic (`PurchaseInventoryUseCase`, `ConfirmPurchaseUseCase`, `HandleProductCreatedUseCase`, `UpdateInventoryUseCase`)
- Domain behavior and invariants beyond the two repository tests
- Persistence logic in `InventoryRepositoryImpl` and `OutboxRepositoryImpl`
- RabbitMQ inbound/outbound event processing and message serialization
- OpenAPI or security filter behavior under request execution
- End-to-end or integration tests using Quarkus test framework

## 5. Recommended Test Strategy

### 5.1 Unit Tests

Add unit tests for the following layers:

- Application use cases: ensure orchestration of repositories, cache, and outbox publisher
- Domain model: validate `Inventory.decrease()`, `Inventory.addStock()`, and any invariants
- Repository implementations: verify database queries and entity mapping with mocks or test database
- Event producers/consumers: test event envelope serialization, type validation, and handler delegation
- Security filter: validate API key acceptance and rejection logic

### 5.2 Integration Tests

Introduce integration tests that leverage Quarkus or a lightweight framework to verify:

- REST endpoints wired through Quarkus runtime
- OpenAPI annotations and API contract correctness
- Persistence against a test PostgreSQL database
- Redis behavior against a local Redis instance or Testcontainers
- RabbitMQ message flow using a broker emulator or Testcontainers

### 5.3 Test Data and Seeding

Use small, deterministic fixtures for domain objects and expected JSON payloads.
Prefer factories that allow overriding only the fields required for the scenario.

### 5.4 Failure and Safety Modes

Assert both happy-path and failure modes:

- Resource not found
- Insufficient stock
- Invalid reservation data
- External infrastructure failure (Redis/RabbitMQ down)
- Serialization errors

## 6. Test Execution

Run the full test suite with Maven:

```bash
mvn test
```

Run a single test class:

```bash
mvn -Dtest=InventoryControllerTest test
```

Run test coverage in a CI pipeline with:

```bash
mvn test jacoco:report
```

## 7. Best Practices for This Repository

- Keep test classes small, focused, and expressive
- Use meaningful display names and comments for scenario clarity
- Mock only external dependencies and integrations; avoid mocking domain objects under test
- Favor explicit assertions over broad `assertTrue` checks when possible
- Use JSON fixtures or small builders to represent request and response payloads
- Add regression tests for all production bug fixes

## 8. Suggested Next Improvements

1. Add `UseCase` unit tests for `PurchaseInventoryUseCase` and `ConfirmPurchaseUseCase`
2. Add a `Mapper` test to verify request-to-command and domain-to-response conversion
3. Add integration tests using Quarkus `@QuarkusTest` or Testcontainers for Redis and PostgreSQL
4. Add a contract test for the API key filter and OpenAPI security enforcement
5. Add coverage reporting into CI to enforce a minimum threshold

## 9. Current Test File Summary

| File | Layer | Purpose | Notes |
| --- | --- | --- | --- |
| `InventoryControllerTest.java` | Web / controller | Validate HTTP layer behavior and response mapping | Uses Mockito and JUnit 5 |
| `ReservationRedisRepositoryTest.java` | Infrastructure / Redis | Validate Redis persistence adapter, validation, and resilience | Focuses on Redis hash behavior and input guarding |

## 10. How to Extend Tests

To add a new test file:

1. Create a test class in `src/test/java` under the matching package.
2. Use `@ExtendWith(MockitoExtension.class)` for unit tests.
3. Mock external dependencies and `@InjectMocks` for the class under test.
4. Arrange / Act / Assert clearly in each test method.
5. Keep tests deterministic and avoid network or file I/O in unit tests.

---

This document is intended to support a robust test roadmap for the inventory service and guide future quality improvements.