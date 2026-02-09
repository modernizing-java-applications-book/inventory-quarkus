# Inventory Quarkus API

A RESTful inventory management microservice built with Quarkus, providing CRUD operations for inventory items with pagination, validation, security, metrics, resilience, and OpenAPI documentation.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [API Endpoints](#api-endpoints)
- [API Versioning](#api-versioning)
- [Security](#security)
- [Metrics](#metrics)
- [Resilience](#resilience)
- [Data Model](#data-model)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Technology Stack](#technology-stack)
- [Configuration](#configuration)
- [CI/CD](#cicd)

## Features

- ✅ Full CRUD operations for inventory items
- ✅ **API Versioning (v1 endpoints with enhanced features)**
- ✅ **Metrics with Micrometer/Prometheus**
- ✅ **Resilience patterns (Circuit Breaker, Retry, Timeout)**
- ✅ Paginated list endpoint with metadata
- ✅ Search inventory by product ID
- ✅ Bean Validation for input data
- ✅ OpenAPI/Swagger UI documentation
- ✅ Health checks (liveness and readiness)
- ✅ Comprehensive error handling with consistent error responses
- ✅ Caching with Caffeine for improved performance
- ✅ JWT-based authentication with role-based access control
- ✅ PostgreSQL support for production with Flyway migrations
- ✅ Structured JSON logging for production
- ✅ Audit fields (createdAt, updatedAt)
- ✅ H2 in-memory database for development
- ✅ Native image compilation support
- ✅ **CI/CD Pipeline with Jenkins**

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL (for production)
- (Optional) GraalVM for native compilation

## Running the Application

### Development Mode

```bash
./mvnw quarkus:dev
```

The application will start at `http://localhost:8080`.

Authentication is disabled in development mode for easier testing.

### Production Mode

```bash
# Build the application
./mvnw clean package

# Run with PostgreSQL
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=inventory
export POSTGRES_USER=inventory
export POSTGRES_PASSWORD=inventory
export JWT_ISSUER=https://your-issuer.com
export JWT_PUBLIC_KEY_URL=/path/to/publicKey.pem

java -jar target/quarkus-app/quarkus-run.jar
```

### Native Mode

```bash
./mvnw package -Dnative
./target/inventory-quarkus-1.0.0-SNAPSHOT-runner
```

### Docker

```bash
# Build Docker image
docker build -t inventory-quarkus .

# Run with Docker
docker run -i --rm -p 8080:8080 \
  -e POSTGRES_HOST=host.docker.internal \
  -e POSTGRES_USER=inventory \
  -e POSTGRES_PASSWORD=inventory \
  inventory-quarkus
```

## API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/q/swagger-ui
- **OpenAPI Spec (YAML)**: http://localhost:8080/q/openapi
- **OpenAPI Spec (JSON)**: http://localhost:8080/q/openapi?format=json

## API Endpoints

### Original API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/inventory` | List inventory (paginated) |
| GET | `/api/inventory/all` | List all inventory |
| GET | `/api/inventory/count` | Count inventory items |
| GET | `/api/inventory/{id}` | Get by ID |
| GET | `/api/inventory/product/{id}` | Get by product ID |
| POST | `/api/inventory` | Create item |
| PUT | `/api/inventory/{id}` | Update item |
| PATCH | `/api/inventory/{id}/quantity` | Update quantity |
| DELETE | `/api/inventory/{id}` | Delete item |
| DELETE | `/api/inventory/cache` | Clear caches |

### List Inventory Items (Paginated)

```http
GET /api/inventory?page=0&size=20
```

**Response:**
```json
{
  "data": [
    {
      "id": 100000,
      "productId": 1001,
      "quantity": 0,
      "createdAt": "2026-02-09T00:00:00Z",
      "updatedAt": "2026-02-09T00:00:00Z"
    }
  ],
  "total": 8,
  "page": 0,
  "size": 20,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

## API Versioning

This API uses URI path versioning. Two versions are available:

### Version 1 (Enhanced) - `/api/v1/inventory`

The v1 endpoints include additional features:

| Feature | Description |
|---------|-------------|
| **Metrics** | All endpoints are instrumented with `@Counted` and `@Timed` annotations |
| **Timeout** | 2-5 second timeouts on read operations |
| **Circuit Breaker** | Opens after 50% failure rate (10 requests window) |
| **Retry** | 3 retries with 100ms delay for get operations |

### V1 Endpoints

```http
GET /api/v1/inventory              # List (Circuit Breaker + Metrics)
GET /api/v1/inventory/{id}         # Get by ID (Retry + Timeout + Cache)
GET /api/v1/inventory/product/{id} # Get by product (Retry + Cache)
POST /api/v1/inventory             # Create (Metrics)
PUT /api/v1/inventory/{id}         # Update (Metrics)
PATCH /api/v1/inventory/{id}/quantity # Update quantity (Metrics)
DELETE /api/v1/inventory/{id}      # Delete (Metrics)
```

### Version Compatibility

| Version | Status | Features |
|---------|--------|----------|
| v0 (unversioned) | Stable | Basic CRUD, caching |
| v1 | Current | Metrics, resilience patterns, caching |

## Security

### JWT Authentication

This API uses JWT (JSON Web Token) authentication for production. The authentication is disabled in development mode.

### Roles

| Role | Permissions |
|------|-------------|
| `admin` | Full access: Create, Read, Update, Delete, Clear Cache |
| `inventory-manager` | Create, Read, Update |
| `inventory-viewer` | Read, Update Quantity only |

### Endpoint Security Matrix

| Endpoint | Method | Required Role |
|----------|--------|---------------|
| `/api/inventory` | GET | Public (No auth) |
| `/api/inventory/all` | GET | Public (No auth) |
| `/api/inventory/count` | GET | Public (No auth) |
| `/api/inventory/{id}` | GET | Public (No auth) |
| `/api/inventory/product/{id}` | GET | Public (No auth) |
| `/api/inventory` | POST | `admin`, `inventory-manager` |
| `/api/inventory/{id}` | PUT | `admin`, `inventory-manager` |
| `/api/inventory/{id}/quantity` | PATCH | `admin`, `inventory-manager`, `inventory-viewer` |
| `/api/inventory/{id}` | DELETE | `admin` only |
| `/api/inventory/cache` | DELETE | `admin` only |
| `/q/health/*` | GET | Public (No auth) |

### JWT Token Configuration

Configure JWT in production with environment variables:

```bash
export JWT_ISSUER=https://your-identity-provider.com
export JWT_PUBLIC_KEY_URL=https://your-identity-provider.com/.well-known/jwks.json
```

## Metrics

### Prometheus Metrics

The application exposes Prometheus-compatible metrics at `/q/metrics`.

#### Custom Metrics

| Metric Name | Type | Description |
|-------------|------|-------------|
| `inventory.list.count` | Counter | Total list requests |
| `inventory.list.timer` | Timer | List request duration |
| `inventory.get.by.id.count` | Counter | Get by ID requests |
| `inventory.get.by.id.timer` | Timer | Get by ID duration |
| `inventory.get.by.product.count` | Counter | Get by product requests |
| `inventory.create.count` | Counter | Create operations |
| `inventory.create.timer` | Timer | Create duration |
| `inventory.update.count` | Counter | Update operations |
| `inventory.delete.count` | Counter | Delete operations |
| `inventory.total.items` | Gauge | Total inventory items |

#### Example Metrics Output

```
# HELP inventory_list_count_total Total list requests
# TYPE inventory_list_count_total counter
inventory_list_count_total 10.0

# HELP inventory_list_timer_seconds List request duration
# TYPE inventory_list_timer_seconds summary
inventory_list_timer_seconds{quantile="0.5"} 0.015
inventory_list_timer_seconds{quantile="0.95"} 0.025
inventory_list_timer_seconds{quantile="0.99"} 0.030
inventory_list_timer_seconds_count 10.0
```

#### Accessing Metrics

```bash
curl http://localhost:8080/q/metrics
```

### Grafana Dashboard

Use the Prometheus metrics with Grafana for visualization. Key panels:
- Request rate (requests/second)
- Response time percentiles (p50, p95, p99)
- Error rate
- Circuit breaker status

## Resilience

### Circuit Breaker

Protects against cascading failures by opening when failure threshold is reached.

**Configuration:**
- Request Volume Threshold: 10 requests
- Failure Ratio: 50%
- Delay: 5 seconds
- Success Threshold: 3 successful calls

**Applied to:** `GET /api/v1/inventory`

```java
@CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000, successThreshold = 3)
```

### Timeout

Prevents hanging requests by timing out after specified duration.

| Endpoint | Timeout |
|----------|---------|
| List inventory | 5 seconds |
| List all | 3 seconds |
| Get by ID | 2 seconds |
| Get by product | 2 seconds |

### Retry

Automatically retries failed operations.

**Configuration:**
- Max Retries: 3
- Delay: 100ms

**Applied to:** Get by ID, Get by Product

```java
@Retry(maxRetries = 3, delay = 100)
```

## Data Model

### Inventory

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | Auto-generated | Unique identifier |
| productId | Long | Required, Unique | Associated product ID |
| quantity | int | Required, Min 0 | Available quantity |
| createdAt | Instant | Auto-set | Creation timestamp |
| updatedAt | Instant | Auto-updated | Last update timestamp |

### Example JSON

```json
{
  "id": 329299,
  "productId": 1002,
  "quantity": 35,
  "createdAt": "2026-02-09T00:00:00.000Z",
  "updatedAt": "2026-02-09T00:00:00.000Z"
}
```

## Error Handling

All errors return a consistent JSON structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Inventory item not found with id: 999999",
  "path": "/api/inventory/999999",
  "timestamp": "2026-02-09T00:00:00.000Z"
}
```

### HTTP Status Codes

| Status | Description |
|--------|-------------|
| 200 | Success (GET, PUT, PATCH) |
| 201 | Created (POST) |
| 204 | No Content (DELETE) |
| 400 | Bad Request - Validation error |
| 401 | Unauthorized - Missing or invalid JWT |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 415 | Unsupported Media Type |
| 500 | Internal Server Error |
| 503 | Service Unavailable - Circuit breaker open |

## Testing

### Run All Tests

```bash
./mvnw test
```

### Run Integration Tests

```bash
./mvnw verify -Dnative
```

### Test Categories

| Test Class | Count | Description |
|------------|-------|-------------|
| `InventoryResourceTest.java` | 30 | Original API tests |
| `InventoryResourceV1Test.java` | 21 | V1 API tests with metrics |
| `NativeInventoryResourceIT.java` | - | Native image tests |

## Technology Stack

| Category | Technology |
|----------|------------|
| Framework | Quarkus 3.8.4 |
| Language | Java 17 |
| JAX-RS | RESTEasy Reactive |
| ORM | Hibernate ORM with Panache |
| Database | H2 (dev), PostgreSQL (prod) |
| Migrations | Flyway |
| Validation | Hibernate Validator |
| Security | SmallRye JWT |
| Documentation | SmallRye OpenAPI with Swagger UI |
| Health | SmallRye Health |
| Caching | Quarkus Cache with Caffeine |
| **Metrics** | **Micrometer with Prometheus** |
| **Resilience** | **SmallRye Fault Tolerance** |
| Logging | Quarkus Logging JSON |
| Testing | JUnit 5, Rest Assured |

## Project Structure

```
src/
├── main/
│   ├── java/com/redhat/cloudnative/
│   │   ├── Inventory.java              # Entity class
│   │   ├── InventoryResource.java      # REST endpoints (unversioned)
│   │   ├── InventoryResourceV1.java    # REST endpoints v1 (metrics + resilience)
│   │   ├── PaginatedResponse.java      # Pagination wrapper
│   │   ├── QuantityUpdateRequest.java  # DTO for PATCH
│   │   ├── ErrorResponse.java          # Error response DTO
│   │   └── ...ExceptionMappers.java    # Exception handlers
│   └── resources/
│       ├── application.properties      # Configuration
│       ├── import.sql                  # Seed data (dev)
│       └── db/migration/               # Flyway migrations
│           └── V1.0.0__Initial_schema.sql
├── test/
│   └── java/com/redhat/cloudnative/
│       ├── InventoryResourceTest.java     # Original API tests
│       └── InventoryResourceV1Test.java   # V1 API tests
└── CICD/
    └── Pipelines/
        └── Jenkinsfile                # CI/CD Pipeline
```

## Caching

This application uses Quarkus Cache with Caffeine backend for improved performance on read operations.

### Cached Endpoints

| Endpoint | Cache Name | Description |
|----------|------------|-------------|
| `GET /api/inventory/{itemId}` | `inventory-cache` | Cached by inventory ID |
| `GET /api/inventory/product/{productId}` | `inventory-product-cache` | Cached by product ID |

### Cache Configuration

```properties
# Cache expires after 5 minutes
quarkus.cache.caffeine.inventory-cache.expire-after-write=5m
quarkus.cache.caffeine.inventory-product-cache.expire-after-write=5m
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_HOST` | localhost | PostgreSQL host |
| `POSTGRES_PORT` | 5432 | PostgreSQL port |
| `POSTGRES_DB` | inventory | Database name |
| `POSTGRES_USER` | inventory | Database user |
| `POSTGRES_PASSWORD` | inventory | Database password |
| `JWT_ISSUER` | - | JWT token issuer URL |
| `JWT_PUBLIC_KEY_URL` | - | URL to JWT public key |

### Development Configuration

```properties
# H2 in-memory database
quarkus.datasource.jdbc.url=jdbc:h2:mem:inventory
quarkus.datasource.db-kind=h2

# Security disabled for development
%dev.quarkus.smallrye-jwt.enabled=false

# Metrics enabled
quarkus.micrometer.enabled=true
```

### Production Configuration

```properties
# PostgreSQL database
%prod.quarkus.datasource.db-kind=postgresql

# Flyway migrations
%prod.quarkus.flyway.migrate-at-start=true

# JWT Security
%prod.mp.jwt.verify.issuer=${JWT_ISSUER}
%prod.quarkus.smallrye-jwt.enabled=true

# JSON Logging
%prod.quarkus.log.console.json=true

# Metrics
quarkus.micrometer.export.prometheus.enabled=true
```

## Observability Endpoints

| Endpoint | Description |
|----------|-------------|
| `/q/health` | Overall health status |
| `/q/health/ready` | Readiness probe |
| `/q/health/live` | Liveness probe |
| `/q/metrics` | Prometheus metrics |
| `/q/swagger-ui` | API documentation |
| `/q/openapi` | OpenAPI specification |

## CI/CD

A complete Jenkins pipeline is provided in `CICD/Pipelines/Jenkinsfile`.

### Pipeline Stages

1. **Checkout** - Clone source code
2. **Validate** - Code style & dependency checks
3. **Build** - Compile with Maven
4. **Unit Tests** - Run tests with coverage
5. **SonarQube** - Static code analysis (optional)
6. **Package** - Build JAR artifacts
7. **Docker Build** - Create container image
8. **Security Scan** - Trivy vulnerability scan
9. **Deploy Dev** - Auto-deploy on develop branch
10. **Deploy Staging** - Manual approval required
11. **Deploy Production** - Manual approval required

### Branch Strategy

| Branch | Deployment |
|--------|------------|
| `develop` | Development environment |
| `main/master` | Staging → Production |
| `feature/*` | Build and test only |

### Required Jenkins Plugins

- Pipeline, Git, Docker Pipeline
- Kubernetes CLI, Credentials Binding
- SonarQube Scanner, Email Extension
- Slack Notification

See `CICD/README.md` for detailed configuration.

## Database Migrations

This project uses Flyway for database migrations in production.

### Creating a New Migration

1. Create a file in `src/main/resources/db/migration/`
2. Name it with version pattern: `V1.0.1__Description.sql`
3. Write your SQL migration

### Example Migration

```sql
-- V1.0.1__Add_low_stock_flag.sql
ALTER TABLE INVENTORY ADD COLUMN low_stock_threshold INTEGER DEFAULT 10;
```

## License

This project is licensed under the Apache License 2.0.