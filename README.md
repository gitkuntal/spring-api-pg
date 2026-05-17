# Spring Boot Product API

A production-ready REST API for product management built with **Spring Boot 3**, **PostgreSQL**, and **Redis** — fully containerised with Docker Compose.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Build Tool | Maven 3.9 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| ORM | Spring Data JPA / Hibernate 6 |
| Validation | Jakarta Bean Validation |
| Boilerplate | Lombok |
| Container | Docker + Docker Compose |

---

## Architecture

```
┌─────────────────────────────────────────┐
│              HTTP Client                │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│           ProductController             │  REST layer
│         /api/products/**                │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│          ProductServiceImpl             │  Business logic
│    @Cacheable / @CachePut / @CacheEvict │◄──── Redis
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│          ProductRepository              │  Data access
│          (JpaRepository)               │◄──── PostgreSQL
└─────────────────────────────────────────┘
```

### Package layout

```
src/main/java/com/example/productapi/
├── ProductApiApplication.java
├── config/
│   └── RedisConfig.java
├── controller/
│   └── ProductController.java
├── service/
│   ├── ProductService.java
│   └── ProductServiceImpl.java
├── repository/
│   └── ProductRepository.java
├── model/
│   └── Product.java
└── exception/
    ├── ProductNotFoundException.java
    └── GlobalExceptionHandler.java
```

---

## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose)

### Run with Docker Compose

```bash
# Clone the repository
git clone https://github.com/gitkuntal/spring-api-pg.git
cd spring-api-pg

# Build and start all services
docker compose up --build
```

The app will be available at **http://localhost:8080** once all three services are healthy.

| Service | Port | Container name |
|---------|------|----------------|
| Spring Boot API | 8080 | `product-api` |
| PostgreSQL | 5432 | `product-postgres` |
| Redis | 6379 | `product-redis` |

### Run infrastructure only (local dev)

```bash
# Start only PostgreSQL + Redis
docker compose up postgres redis

# Run the app locally
mvn spring-boot:run
```

### Build the JAR

```bash
mvn clean package -DskipTests
java -jar target/product-api-1.0.0.jar
```

---

## API Reference

Base URL: `http://localhost:8080/api/products`

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | List all products |
| `GET` | `/{id}` | Get product by ID |
| `POST` | `/` | Create a product |
| `PUT` | `/{id}` | Update a product |
| `DELETE` | `/{id}` | Delete a product |
| `GET` | `/category/{category}` | Filter by category |
| `GET` | `/search?name={name}` | Search by name (case-insensitive) |

### Product schema

```json
{
  "id": 1,
  "name": "Wireless Mouse",
  "price": 29.99,
  "description": "Ergonomic wireless mouse with long battery life",
  "category": "Electronics",
  "stockQuantity": 150,
  "brand": "Logitech",
  "sku": "LGT-MS-001",
  "createdAt": "2026-05-17T10:00:00",
  "updatedAt": "2026-05-17T10:00:00"
}
```

**Required fields:** `name`, `price`

### Example requests

**Create a product**
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse",
    "price": 29.99,
    "description": "Ergonomic wireless mouse",
    "category": "Electronics",
    "stockQuantity": 150,
    "brand": "Logitech",
    "sku": "LGT-MS-001"
  }'
```

**Get all products**
```bash
curl http://localhost:8080/api/products
```

**Get by ID**
```bash
curl http://localhost:8080/api/products/1
```

**Update a product**
```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse Pro",
    "price": 39.99,
    "category": "Electronics",
    "stockQuantity": 120,
    "brand": "Logitech",
    "sku": "LGT-MS-001"
  }'
```

**Delete a product**
```bash
curl -X DELETE http://localhost:8080/api/products/1
```

**Filter by category**
```bash
curl http://localhost:8080/api/products/category/Electronics
```

**Search by name**
```bash
curl "http://localhost:8080/api/products/search?name=mouse"
```

---

## Caching

Redis caching is applied in the service layer using Spring Cache annotations. TTL is **10 minutes**.

| Cache | Key | Evicted by |
|-------|-----|------------|
| `product` | `{id}` | update / delete of that ID |
| `products` | _(all)_ | any create, update, or delete |
| `productsByCategory` | `{category}` | manual flush |

**Inspect cache keys at runtime:**
```bash
docker exec product-redis redis-cli keys "*"
```

---

## Configuration

All values are environment-variable driven with sensible local defaults in `application.yml`.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `productdb` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | _(empty)_ | Redis password |

---

## Docker architecture

```
docker-compose.yml
├── postgres  postgres:16-alpine   port 5432  volume: postgres_data
├── redis     redis:7-alpine       port 6379  volume: redis_data (AOF enabled)
└── app       (multi-stage build)  port 8080  waits for healthy postgres + redis
```

The `Dockerfile` uses a two-stage build:
1. **Build** — `maven:3.9.6-eclipse-temurin-21` compiles and packages the JAR
2. **Runtime** — `eclipse-temurin:21-jre-alpine` runs as a non-root user for a minimal, secure image

---

## Useful commands

```bash
# View live app logs
docker logs -f product-api

# Connect to PostgreSQL
docker exec -it product-postgres psql -U postgres -d productdb

# Open Redis CLI
docker exec -it product-redis redis-cli

# Stop all containers and remove volumes
docker compose down -v
```

---

## Error responses

| Scenario | HTTP Status |
|----------|------------|
| Product not found | `404 Not Found` |
| Validation failure | `400 Bad Request` (field-level errors map) |
| Unexpected error | `500 Internal Server Error` |

**404 example:**
```json
{
  "status": 404,
  "message": "Product not found with id: 99",
  "timestamp": "2026-05-17T10:00:00"
}
```
