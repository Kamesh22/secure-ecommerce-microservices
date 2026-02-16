# Inventory Service

Production-grade Inventory Management Microservice for E-Commerce Platform

## Overview

The Inventory Service microservice manages product stock and reservations. It handles:
- Creating and managing inventory records
- Reserving stock for orders
- Releasing reserved stock (cancellations)
- Confirming final stock deductions

## Technology Stack

- **Java 17** with Spring Boot 3.2.5
- **Spring Cloud** for Eureka registration and Config Server integration
- **PostgreSQL** for data persistence
- **MapStruct** for entity-to-DTO mapping
- **Lombok** for boilerplate reduction
- **Spring Security** for role-based access control
- **Springdoc** for OpenAPI/Swagger documentation
- **Spring Data JPA** with Hibernate ORM
- **Flyway** for database migrations

## Architecture

### Entity Model

**Inventory Table**
- `id`: Primary key (BIGINT)
- `product_id`: Unique product identifier (BIGINT, UNIQUE)
- `available_quantity`: Available stock (INT, >= 0)
- `reserved_quantity`: Reserved stock for pending orders (INT, >= 0)
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp

**Business Logic**
- Total stock = `availableQuantity` + `reservedQuantity`
- Reserve: `availableQuantity` decreases, `reservedQuantity` increases
- Release: `availableQuantity` increases, `reservedQuantity` decreases
- Confirm: `reservedQuantity` decreases (stock permanently removed)

### Layered Architecture

```
Controller Layer → Service Layer → Repository Layer → Database
```

- **Controllers**: Handle HTTP requests, validate inputs, enforce role-based access
- **Services**: Implement business logic, transactions, validation
- **Repositories**: JPA-based data access
- **Entities**: Domain models with JPA annotations

## API Endpoints

### Public Endpoints
```
GET /api/inventory/{productId}              - Get inventory by product ID
```

### Admin Endpoints (Role: ADMIN)
```
POST   /api/inventory                       - Create new inventory
PUT    /api/inventory/{productId}           - Update available quantity
GET    /api/inventory                       - List all inventory
```

### Internal Endpoints (Role: ORDER_SERVICE)
```
POST   /api/inventory/reserve               - Reserve stock for order
POST   /api/inventory/release               - Release reserved stock
POST   /api/inventory/confirm               - Confirm reservation
```

## Security

### Authentication & Authorization

- **No local login**: Authentication handled by API Gateway
- **Role-based access via headers**:
  - `X-User-Roles`: Comma-separated roles (e.g., "ADMIN,USER")
  - `X-User-Id`: User identifier (optional)
  - `X-Internal-Call`: Marker for internal service calls

### Access Control

- Role extraction happens in `RoleExtractionFilter` (SecurityConfig)
- Roles are converted to Spring Security `GrantedAuthority` objects
- Method-level security via `@PreAuthorize` annotations

### Roles

- **ADMIN**: Full access (create, update, list, read)
- **ORDER_SERVICE**: Internal role for stock operations (reserve, release, confirm)
- **PUBLIC**: No role required for GET inventory by product ID

## Configuration

### Bootstrap Configuration (Local)
File: `src/main/resources/application.yml`
```yaml
spring:
  application:
    name: inventory-service
  config:
    import: optional:configserver:http://localhost:8888

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Server Configuration (Config Server)
File: `config-repo/inventory-service.yml`
- Database connection (PostgreSQL)
- JPA/Hibernate settings
- Actuator exposure
- CORS configuration
- OpenAPI/Swagger settings

## Building & Running

### Build
```bash
mvn clean package
```

### Run Locally
```bash
mvn spring-boot:run
```

### Run with Docker
```bash
docker build -t inventory-service:1.0.0 .
docker run -p 8082:8082 \
  -e SPRING_CLOUD_CONFIG_URI=http://config-server:8888 \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-server:8761/eureka/ \
  inventory-service:1.0.0
```

## API Documentation

Swagger UI available at: `http://localhost:8082/swagger-ui.html`

API Docs JSON: `http://localhost:8082/v3/api-docs`

## Health & Monitoring

- Health check: `http://localhost:8082/actuator/health`
- Application info: `http://localhost:8082/actuator/info`
- Metrics: `http://localhost:8082/actuator/metrics`

## Dependencies

Inherited from parent POM:
- Spring Boot 3.2.5
- Spring Cloud 2023.0.2
- PostgreSQL driver 42.7.8
- MapStruct 1.5.5
- Lombok 1.18.38
- Springdoc OpenAPI 2.5.0

## Eureka Registration

Service automatically registers with Eureka Discovery Server:
- **Service Name**: `inventory-service`
- **Health Check**: `/actuator/health`
- **Instance ID**: `inventory-service:8082`

## Error Handling

Global exception handling via `GlobalExceptionHandler` (from common-lib):
- `ResourceNotFoundException`: 404 Not Found
- `BusinessException`: 400 Bad Request
- `MethodArgumentNotValidException`: 400 Bad Request
- `Exception`: 500 Internal Server Error

## Project Structure

```
inventory-service/
├── src/main/java/com/ecommerce/inventory/
│   ├── InventoryServiceApplication.java
│   ├── controller/
│   │   └── InventoryController.java
│   ├── service/
│   │   ├── InventoryService.java
│   │   └── InventoryServiceImpl.java
│   ├── entity/
│   │   └── Inventory.java
│   ├── dto/
│   │   ├── InventoryRequestDTO.java
│   │   ├── InventoryResponseDTO.java
│   │   ├── ReserveRequestDTO.java
│   │   ├── ReleaseRequestDTO.java
│   │   └── ConfirmRequestDTO.java
│   ├── mapper/
│   │   └── InventoryMapper.java
│   ├── repository/
│   │   └── InventoryRepository.java
│   └── config/
│       ├── SecurityConfig.java
│       └── OpenAPIConfig.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__Create_inventory_table.sql
├── pom.xml
├── Dockerfile
└── README.md
```

## Common Library Integration

Imports shared exception handling and utilities from `common-lib`:
- `GlobalExceptionHandler`: REST error response handling
- `ResourceNotFoundException`: Not found exception
- `BusinessException`: Business rule violations
- `ErrorResponse`: Standardized error response DTO
- `RoleExtractor`: Header-based role extraction utility

## Development Notes

- Constructor injection only (no field injection)
- Immutable DTOs using Lombok `@Data`
- Transactional service methods
- SLF4J logging throughout
- No hardcoded configuration values
- Type-safe MapStruct mapping

## Future Enhancements

- Event-driven architecture for stock changes
- Caching layer for frequent queries
- Stock movement audit trail
- Notification service integration
- Advanced reservation policies
