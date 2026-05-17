# Product API — CLAUDE.md

Spring Boot 3.2.5 REST API for product management using PostgreSQL (persistence) and Redis (caching). All services run in Docker via Docker Compose.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Build | Maven 3.9 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Boilerplate | Lombok |
| Container | Docker + Docker Compose |

---

## Project Layout

```
src/main/java/com/example/productapi/
├── ProductApiApplication.java     # Entry point — @EnableCaching is here
├── config/
│   └── RedisConfig.java           # RedisTemplate + CacheManager beans
├── controller/
│   └── ProductController.java     # REST layer — @RestController
├── service/
│   ├── ProductService.java        # Interface
│   └── ProductServiceImpl.java    # Cache annotations live here
├── repository/
│   └── ProductRepository.java     # JpaRepository + custom finders
├── model/
│   └── Product.java               # JPA entity + Bean Validation
└── exception/
    ├── ProductNotFoundException.java
    └── GlobalExceptionHandler.java  # @RestControllerAdvice
```

---

## REST Endpoints

Base path: `/api/products`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | List all products |
| GET | `/{id}` | Get product by ID |
| POST | `/` | Create product |
| PUT | `/{id}` | Replace product |
| DELETE | `/{id}` | Delete product |
| GET | `/category/{category}` | Filter by category |
| GET | `/search?name=x` | Search by name (case-insensitive) |

---

## Product Model

```json
{
  "id": 1,
  "name": "Widget Pro",
  "price": 29.99,
  "description": "A high-quality widget",
  "category": "Electronics",
  "stockQuantity": 100,
  "brand": "Acme",
  "sku": "WGT-001",
  "createdAt": "2026-05-17T10:00:00",
  "updatedAt": "2026-05-17T10:00:00"
}
```

Required fields on create/update: `name`, `price`.

---

## Caching Strategy (Redis)

Cache is managed in `ProductServiceImpl` using Spring Cache annotations. TTL is **10 minutes**.

| Cache name | Key | Invalidated by |
|------------|-----|----------------|
| `products` | _(single entry)_ | create, update, delete |
| `product` | `{id}` | update, delete of that ID |
| `productsByCategory` | `{category}` | not auto-evicted on write |

Redis serialization uses `GenericJackson2JsonRedisSerializer` with `JavaTimeModule` for `LocalDateTime` support.

---

## Configuration

All values are environment-variable driven with local defaults:

| Env Var | Default | Purpose |
|---------|---------|---------|
| `DB_HOST` | `localhost` | Postgres host |
| `DB_PORT` | `5432` | Postgres port |
| `DB_NAME` | `productdb` | Database name |
| `DB_USER` | `postgres` | DB username |
| `DB_PASSWORD` | `postgres` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | _(empty)_ | Redis password |

See [src/main/resources/application.yml](src/main/resources/application.yml) for full config.

---

## Common Commands

### Run everything in Docker
```bash
docker compose up --build
```

### Run only infrastructure (develop the app locally)
```bash
docker compose up postgres redis
```

### Build the JAR locally
```bash
mvn clean package -DskipTests
```

### Run tests
```bash
mvn test
```

### Stop and remove containers + volumes
```bash
docker compose down -v
```

### Tail app logs
```bash
docker logs -f product-api
```

### Connect to Postgres directly
```bash
docker exec -it product-postgres psql -U postgres -d productdb
```

### Check Redis cache keys
```bash
docker exec -it product-redis redis-cli keys "*"
```

---

## Docker Architecture

```
docker-compose.yml
├── postgres  (postgres:16-alpine)  — port 5432, volume: postgres_data
├── redis     (redis:7-alpine)      — port 6379, volume: redis_data, AOF enabled
└── app       (multi-stage build)   — port 8080, waits for healthy postgres + redis
```

The `app` service uses a two-stage Dockerfile:
1. **Build stage** — `maven:3.9.6-eclipse-temurin-21` compiles and packages the JAR.
2. **Runtime stage** — `eclipse-temurin:21-jre-alpine` runs as a non-root user (`appuser`).

---

## Error Handling

`GlobalExceptionHandler` maps exceptions to HTTP responses:

| Exception | HTTP Status |
|-----------|------------|
| `ProductNotFoundException` | 404 Not Found |
| `MethodArgumentNotValidException` | 400 Bad Request (field errors map) |
| Any other `Exception` | 500 Internal Server Error |

---

## Key Design Notes

- `Product` implements `Serializable` — required for Redis serialization.
- `@PrePersist` / `@PreUpdate` on the entity manage `createdAt` / `updatedAt` automatically.
- `ddl-auto: update` — Hibernate manages the schema; suitable for development. Switch to `validate` with Flyway/Liquibase for production.
- `productsByCategory` cache is **not** evicted on product writes — flush manually via Redis CLI or extend eviction logic if stale reads are a concern.
