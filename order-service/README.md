# Order Service

Production-grade Order Management Microservice for E-Commerce Platform

## Overview

The Order Service microservice handles the complete order lifecycle including:
- Creating orders with multiple items
- Reserving inventory stock
- Processing payment outcomes
- Confirming or canceling orders
- Managing order history

## Technology Stack

- **Java 17** with Spring Boot 3.2.5
- **Spring Cloud** for Eureka registration and Config Server integration
- **WebClient** for synchronous service-to-service communication (Inventory Service)
- **PostgreSQL** for data persistence
- **MapStruct** for entity-to-DTO mapping
- **Lombok** for boilerplate reduction
- **Spring Security** for role-based access control
- **Springdoc** for OpenAPI/Swagger documentation
- **Spring Data JPA** with Hibernate ORM
- **Flyway** for database migrations

## Architecture

### Entity Model

**Order Table**
- `id`: Primary key (BIGINT)
- `user_id`: User who placed the order (BIGINT)
- `status`: Order status enum (VARCHAR) - CREATED, RESERVED, PAID, FAILED, CANCELLED
- `total_amount`: Total order amount (NUMERIC)
- `payment_success`: Whether payment succeeded (BOOLEAN)
- `created_at`: Creation timestamp
- `updated_at`: Last update timestamp

**OrderItem Table**
- `id`: Primary key (BIGINT)
- `order_id`: Foreign key to orders table (BIGINT)
- `product_id`: Product in the order (BIGINT)
- `quantity`: Item quantity (INTEGER)
- `price`: Item price (NUMERIC)

**Relationships**
- Order → OneToMany → OrderItem (cascade delete enabled)

### Layered Architecture

```
HTTP Request
    ↓
Controller (Validation, Security, HTTP)
    ↓
Service (Business Logic, Transactions)
    ↓
Repository (Data Access)
    ↓
Database
    
Plus: WebClient for Inventory Service calls
```

## API Endpoints

### User Endpoints (Roles: USER, ADMIN)
```
POST   /api/orders               - Place a new order
GET    /api/orders/my-orders     - Get current user's orders
GET    /api/orders/{id}          - Get specific order
```

### Admin Endpoints (Role: ADMIN)
```
GET    /api/orders               - List all orders
PUT    /api/orders/{id}/cancel   - Cancel an order
```

## Order Lifecycle

### 1. Place Order Flow

**Input**: OrderRequestDTO with items list and payment success flag

**Steps**:
1. Create Order with status = CREATED
2. For each item:
   - Call Inventory Service to reserve stock
   - Create OrderItem entity
   - Calculate item total
3. Set Order status = RESERVED
4. Check payment success flag:
   - **If TRUE**: Call Inventory confirm → status = PAID
   - **If FALSE**: Call Inventory release → status = FAILED
5. Save and return Order

**Exception Handling**:
- If reserve fails → throw BusinessException
- If confirm fails → release reserved stock, throw BusinessException
- If release fails → throw BusinessException

### 2. Cancel Order Flow

**Only available for ADMIN role**

**Steps**:
1. Find order by ID (404 if not found)
2. If status = RESERVED:
   - Call Inventory release for each item
3. Set status = CANCELLED
4. Save and return Order

## External Integration

### Inventory Service Client

**Methods**:
- `reserveStock(productId, quantity)`: Reserve stock
- `releaseStock(productId, quantity)`: Release reserved stock
- `confirmStock(productId, quantity)`: Confirm reservation

**Configuration**:
- Base URL: `${app.inventory-service.url}` (from config server)
- Uses WebClient for reactive calls (blocking operation)

## Security Model

### Authentication & Authorization

- **No local login**: Authentication handled by API Gateway
- **Role-based access via headers**:
  - `X-User-Roles`: Comma-separated roles (e.g., "USER,ADMIN")
  - `X-User-Id`: Numeric user identifier (used as order owner)

### Access Control

- Role extraction happens in `RoleExtractionFilter` (SecurityConfig)
- Roles converted to Spring Security `GrantedAuthority` objects
- Method-level security via `@PreAuthorize` annotations

### Roles

- **USER**: Can place orders and view own orders
- **ADMIN**: Full access (place orders, view all, cancel)
- **INTERNAL (ORDER_SERVICE)**: For internal service calls

## Configuration

### Bootstrap Configuration (Local)
File: `src/main/resources/application.yml`
```yaml
spring:
  application:
    name: order-service
  config:
    import: optional:configserver:http://localhost:8888

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Server Configuration (Config Server)
File: `config-repo/order-service.yml`
- Database connection (PostgreSQL)
- JPA/Hibernate settings
- Eureka registration
- Actuator exposure
- CORS configuration
- OpenAPI/Swagger settings
- Inventory Service URL

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
docker build -t order-service:1.0.0 .
docker run -p 8083:8083 \
  -e SPRING_CLOUD_CONFIG_URI=http://config-server:8888 \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-server:8761/eureka/ \
  order-service:1.0.0
```

## API Documentation

Swagger UI available at: `http://localhost:8083/swagger-ui.html`

API Docs JSON: `http://localhost:8083/v3/api-docs`

## Health & Monitoring

- Health check: `http://localhost:8083/actuator/health`
- Application info: `http://localhost:8083/actuator/info`
- Metrics: `http://localhost:8083/actuator/metrics`

## Database Schema

**Orders Table**:
- Primary key: `id`
- Indexes: `user_id`, `status`

**OrderItems Table**:
- Primary key: `id`
- Foreign key: `order_id` → orders.id (cascade delete)
- Index: `order_id`

**Migration**: Flyway V1 script (auto-executed)

## Error Handling

Global exception handling via `GlobalExceptionHandler` (from common-lib):
- `ResourceNotFoundException`: 404 Not Found (order not found)
- `BusinessException`: 400 Bad Request (inventory operations fail, validation)
- `MethodArgumentNotValidException`: 400 Bad Request (invalid input)
- `Exception`: 500 Internal Server Error

## Project Structure

```
order-service/
├── src/main/java/com/ecommerce/order/
│   ├── OrderServiceApplication.java
│   ├── controller/
│   │   └── OrderController.java
│   ├── service/
│   │   ├── OrderService.java
│   │   └── OrderServiceImpl.java
│   ├── entity/
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   └── OrderStatus.java
│   ├── dto/
│   │   ├── OrderRequestDTO.java
│   │   ├── OrderResponseDTO.java
│   │   └── OrderItemDTO.java
│   ├── mapper/
│   │   └── OrderMapper.java
│   ├── repository/
│   │   └── OrderRepository.java
│   ├── client/
│   │   ├── InventoryServiceClient.java
│   │   ├── InventoryReserveRequest.java
│   │   ├── InventoryReleaseRequest.java
│   │   ├── InventoryConfirmRequest.java
│   │   └── InventoryResponse.java
│   └── config/
│       ├── SecurityConfig.java
│       ├── WebClientConfig.java
│       └── OpenAPIConfig.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__Create_orders_table.sql
├── pom.xml
├── Dockerfile
└── README.md
```

## Common Library Integration

Uses shared resources from `common-lib`:
- `GlobalExceptionHandler`: REST error response handling
- `ResourceNotFoundException`: Not found exception
- `BusinessException`: Business rule violations
- `ErrorResponse`: Standardized error response DTO
- `RoleExtractor`: Header-based role extraction utility

## Dependencies

- common-lib (local)
- spring-boot-starter-web
- spring-boot-starter-webflux (WebClient)
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-actuator
- spring-cloud-starter-netflix-eureka-client
- spring-cloud-starter-config
- postgresql driver
- flyway-core
- springdoc-openapi-starter-webmvc-ui
- mapstruct
- lombok

## Development Notes

- Constructor injection only (no field injection)
- Immutable DTOs using Lombok `@Data`
- Transactional service methods
- SLF4J logging throughout
- No hardcoded configuration values
- Type-safe MapStruct mapping
- WebClient blocking calls for simplicity

## Eureka Service Registration

**Service Details**:
- **Service Name**: `order-service`
- **Port**: 8083
- **Instance ID**: `order-service:8083`
- **Health Check Path**: `/actuator/health`
- **Status Page**: `/actuator/info`
- **Prefer IP**: true (for Docker compatibility)

## Example Usage

### Place Order
```bash
curl -X POST http://localhost:8083/api/orders \
  -H "X-User-Roles: USER" \
  -H "X-User-Id: 123" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": 1,
        "quantity": 2
      }
    ],
    "paymentSuccess": true
  }'
```

### Get My Orders
```bash
curl http://localhost:8083/api/orders/my-orders \
  -H "X-User-Roles: USER" \
  -H "X-User-Id: 123"
```

### Cancel Order (Admin)
```bash
curl -X PUT http://localhost:8083/api/orders/1/cancel \
  -H "X-User-Roles: ADMIN" \
  -H "X-User-Id: 1"
```

## Future Enhancements

- Asynchronous order processing with Kafka
- Event-driven architecture
- Order status tracking and notifications
- Payment service integration
- Order analytics and reporting
- Refund mechanism
- Order search and filtering
