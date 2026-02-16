# Product Service

A production-ready Spring Boot 4 microservice for managing products in the e-commerce microservices architecture.

## Overview

The Product Service is part of a distributed microservices ecosystem built with Spring Cloud. It handles all product-related operations including creation, retrieval, update, and deletion with proper security, validation, and error handling.

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Java**: Java 17
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Service Discovery**: Netflix Eureka
- **Configuration Management**: Spring Cloud Config
- **Security**: Spring Security with JWT (token validation from API Gateway)
- **Database ORM**: Spring Data JPA
- **Validation**: Jakarta Bean Validation
- **Documentation**: OpenAPI 3.0 / Swagger UI
- **Build Utilities**: Lombok
- **Logging**: SLF4J

## Architecture

### Layered Architecture

```
Controller Layer
    ↓
Service Layer (Interface + Implementation)
    ↓
Repository Layer (JPA)
    ↓
Database (PostgreSQL)
```

### Packages Structure

```
com.ecommerce.product
├── controller          # REST Controllers
├── service            # Business Logic (Interface & Implementation)
├── repository         # Data Access Layer (JPA)
├── entity             # JPA Entities
├── dto                # Data Transfer Objects
├── mapper             # DTO ↔ Entity Mappers
├── exception          # Custom Exceptions & Global Handler
└── config             # Spring Configurations (Security, OpenAPI, etc.)
```

## Database Schema

### Products Table

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Primary key |
| name | VARCHAR(100) | NOT NULL | Product name (3-100 chars) |
| description | TEXT | Max 500 chars | Product description |
| price | DECIMAL(19,2) | NOT NULL, > 0 | Product price |
| quantity | INTEGER | NOT NULL, >= 0 | Available quantity |
| created_at | TIMESTAMP | NOT NULL | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL | Last update timestamp |

## API Endpoints

### Base Path: `/api/products`

#### 1. Create Product
```
POST /api/products
Authorization: Required (ADMIN role)
Content-Type: application/json

Request Body:
{
  "name": "Laptop",
  "description": "High performance laptop",
  "price": 1299.99,
  "quantity": 15
}

Response: 201 Created
{
  "id": 1,
  "name": "Laptop",
  "description": "High performance laptop",
  "price": 1299.99,
  "quantity": 15,
  "createdAt": "2026-02-14T10:30:00",
  "updatedAt": "2026-02-14T10:30:00"
}
```

#### 2. Get All Products
```
GET /api/products?page=0&size=20
Authorization: Not Required

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "name": "Laptop",
      "description": "High performance laptop",
      "price": 1299.99,
      "quantity": 15,
      "createdAt": "2026-02-14T10:30:00",
      "updatedAt": "2026-02-14T10:30:00"
    }
  ],
  "totalElements": 100,
  "page": 0,
  "size": 20,
  "totalPages": 5
}
```

#### 3. Get Product by ID
```
GET /api/products/{id}
Authorization: Not Required

Response: 200 OK
{
  "id": 1,
  "name": "Laptop",
  "description": "High performance laptop",
  "price": 1299.99,
  "quantity": 15,
  "createdAt": "2026-02-14T10:30:00",
  "updatedAt": "2026-02-14T10:30:00"
}
```

#### 4. Update Product
```
PUT /api/products/{id}
Authorization: Required (ADMIN role)
Content-Type: application/json

Request Body:
{
  "name": "Updated Laptop",
  "description": "Updated description",
  "price": 1399.99,
  "quantity": 20
}

Response: 200 OK
{
  "id": 1,
  "name": "Updated Laptop",
  "description": "Updated description",
  "price": 1399.99,
  "quantity": 20,
  "createdAt": "2026-02-14T10:30:00",
  "updatedAt": "2026-02-14T11:00:00"
}
```

#### 5. Delete Product
```
DELETE /api/products/{id}
Authorization: Required (ADMIN role)

Response: 204 No Content
```

#### 6. Search Products
```
GET /api/products/search?name=laptop
Authorization: Not Required

Response: 200 OK
[
  {
    "id": 1,
    "name": "Laptop",
    "description": "High performance laptop",
    "price": 1299.99,
    "quantity": 15,
    "createdAt": "2026-02-14T10:30:00",
    "updatedAt": "2026-02-14T10:30:00"
  }
]
```

## Security

### Authentication
- The service expects JWT tokens to be validated by the **API Gateway**
- Tokens are NOT validated in this service (gateway responsibility)
- The service extracts user roles from the `X-User-Roles` header set by the gateway

### Authorization
- **Public Endpoints**: 
  - `GET /api/products` - List all products
  - `GET /api/products/{id}` - Get product by ID
  - `GET /api/products/search` - Search products
  - `/swagger-ui.html` - Swagger UI
  - `/actuator/health` - Health check
  - `/actuator/info` - Service info

- **Admin-only Endpoints**:
  - `POST /api/products` - Create product
  - `PUT /api/products/{id}` - Update product
  - `DELETE /api/products/{id}` - Delete product

### Role-based Access Control
- Uses `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` annotations
- Roles are extracted from the `X-User-Roles` header

## Configuration

The service loads configuration from the **Config Server** at startup via `bootstrap.yml`:

```yaml
spring:
  application:
    name: product-service
  config:
    import: "optional:configserver:http://localhost:8888"
```

### Configuration Properties (from Config Server - product-service.yml)

- **Database**: PostgreSQL connection details
- **JPA/Hibernate**: Entity mapping and connection pool settings
- **Eureka**: Service discovery and registration
- **Actuator**: Health and metrics endpoints
- **Logging**: Log levels for debugging

## Running the Service

### Prerequisites

1. **Java 17+** installed
2. **Maven 3.8+** installed
3. **PostgreSQL** running on port 5432
4. **Config Server** running on port 8888
5. **Discovery Server (Eureka)** running on port 8761

### Docker Setup

```bash
# Start Docker Compose with PostgreSQL
docker-compose up -d

# This starts PostgreSQL on port 5432 and PgAdmin on port 5050
```

### Build the Project

```bash
# Build parent project (from root)
mvn clean install

# Or build just product-service
cd product-service
mvn clean package
```

### Run the Service

```bash
# Run from IDE or command line
mvn spring-boot:run

# Or run from jar
java -jar target/product-service-1.0.0.jar
```

The service will start on **http://localhost:8081** and automatically:
- Connect to Config Server for external configuration
- Register with Eureka for service discovery
- Connect to PostgreSQL database

### Database Setup

The database needs to be initialized. Run the SQL migration script:

```sql
-- Run this on the product_db database
CREATE DATABASE product_db;
```

Then run the migration script from `src/main/resources/db/migration/V1__init_products_table.sql`

Alternatively, the migration will run automatically if using Flyway (add dependency if needed).

## Health Checks

### Service Health
```
GET http://localhost:8081/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL"
      }
    },
    "discoveryClient": {
      "status": "UP"
    }
  }
}
```

### Service Info
```
GET http://localhost:8081/actuator/info
```

## Swagger UI

Access the interactive API documentation at:

```
http://localhost:8081/swagger-ui.html
```

## Logging

All service operations are logged:
- **DEBUG**: Method entry/exit, detailed operation logs
- **INFO**: Business operations, request summaries
- **WARN**: Potential issues (e.g., resource not found)
- **ERROR**: Exceptions and errors

Example logs are written to console and can be configured to write to files via application configuration.

## Error Handling

All errors follow a consistent format:

```json
{
  "timestamp": "2026-02-14T10:30:00",
  "status": 404,
  "message": "Product not found",
  "details": "Product not found with ID: 999",
  "path": "/api/products/999"
}
```

### Error Codes

| Status | Code | Description |
|--------|------|-------------|
| 201 | CREATED | Resource created successfully |
| 200 | OK | Request successful |
| 204 | NO CONTENT | Resource deleted successfully |
| 400 | BAD REQUEST | Validation error |
| 403 | FORBIDDEN | Access denied (insufficient permissions) |
| 404 | NOT FOUND | Resource not found |
| 500 | INTERNAL SERVER ERROR | Server error |

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

Tests with `@SpringBootTest` annotation that test the complete context.

## Validation Rules

- **Product Name**:
  - Required
  - 3-100 characters
  - Case-sensitive

- **Product Description**:
  - Optional
  - Maximum 500 characters

- **Product Price**:
  - Required
  - Must be greater than 0
  - BigDecimal precision

- **Product Quantity**:
  - Required
  - Minimum 0 (cannot be negative)
  - Integer

## Performance Considerations

1. **Database Indexes**: Indexes on name, price, and created_at for fast queries
2. **Connection Pooling**: HikariCP with pool size 10
3. **Query Optimization**: Named queries and pagination support
4. **Caching**: Can be added using Spring Cache abstractions
5. **Batch Operations**: Hibernate batch size set to 20

## Monitoring

The service exposes metrics via the `/actuator/metrics` endpoint:

```
GET http://localhost:8081/actuator/metrics
```

Monitor:
- HTTP request rates
- Database connection pool status
- Response times
- Error rates

## Troubleshooting

### Service Won't Start

1. Check if Config Server is running on port 8888
2. Check if PostgreSQL is running on port 5432
3. Check if Discovery Server (Eureka) is running on port 8761
4. Review logs: `mvn spring-boot:run` shows verbose output

### Cannot Connect to Database

1. Verify PostgreSQL is running: `psql -U postgres`
2. Verify database `product_db` exists
3. Check database credentials in Config Server configuration
4. Check connection string in `product-service.yml`

### Service Not Registering with Eureka

1. Check if Eureka server is accessible
2. Verify Eureka URL in configuration
3. Check service name in `application.yml`
4. Review Eureka dashboard: `http://localhost:8761`

## Development Notes

### Adding a New Endpoint

1. Add method to `ProductService` interface
2. Implement in `ProductServiceImpl`
3. Add REST endpoint in `ProductController`
4. Add Swagger annotations for documentation
5. Add validation if needed
6. Write unit tests

### Adding a New Feature

Follow the layered architecture:
1. **Controller**: REST endpoint
2. **Service**: Business logic
3. **Repository**: Data access (if needed)
4. **Entity**: Database model (if needed)
5. **DTO**: Data transfer object
6. **Mapper**: Convert between entity and DTO
7. **Exception**: If new error condition
8. **Tests**: Unit and integration tests

## Links and Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Security](https://spring.io/projects/spring-security)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.0)

## License

This project is part of the E-Commerce Microservices ecosystem.

---

**Last Updated**: February 14, 2026  
**Version**: 1.0.0
