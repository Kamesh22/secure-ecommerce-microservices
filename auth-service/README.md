# Auth Service

A JWT-based authentication and authorization microservice for the e-commerce platform. Handles user registration, login, and token validation for secure access to protected resources.

## Overview

The Auth Service provides:
- User registration with email verification
- JWT-based authentication
- Token validation and verification
- Role-based access control (USER, ADMIN)
- Integration with Eureka for service discovery
- Centralized configuration via Config Server

## Features

- **User Registration**: Create new user accounts with username, email, and password
- **Login**: Authenticate users with credentials and receive JWT tokens
- **Token Validation**: Verify and extract information from JWT tokens
- **Role-Based Security**: Support for USER and ADMIN roles
- **Password Encryption**: BCrypt password hashing for secure storage
- **API Documentation**: Swagger/OpenAPI integration for interactive documentation

## Project Structure

```
auth-service/
├── src/main/java/com/ecommerce/auth/
│   ├── controller/
│   │   └── AuthController.java          # REST API endpoints
│   ├── service/
│   │   ├── AuthService.java             # Service interface
│   │   └── impl/
│   │       └── AuthServiceImpl.java      # Service implementation
│   ├── entity/
│   │   ├── User.java                    # User entity
│   │   └── UserRole.java                # Role enum
│   ├── dto/
│   │   ├── RegisterRequestDTO.java      # Registration request
│   │   ├── LoginRequestDTO.java         # Login request
│   │   ├── AuthResponseDTO.java         # Auth response with token
│   │   └── TokenValidationResponseDTO.java  # Token validation response
│   ├── repository/
│   │   └── UserRepository.java          # JPA repository
│   ├── mapper/
│   │   └── UserMapper.java              # MapStruct mapper
│   ├── security/
│   │   └── JwtAuthenticationFilter.java # JWT authentication filter
│   ├── util/
│   │   └── JwtUtil.java                 # JWT token utilities
│   ├── config/
│   │   ├── SecurityConfig.java          # Spring Security configuration
│   │   └── OpenAPIConfig.java           # Swagger configuration
│   └── AuthServiceApplication.java      # Application entry point
├── src/main/resources/
│   ├── application.yml                  # Bootstrap configuration
│   └── db/migration/
│       └── V1__Create_users_table.sql  # Database schema
├── src/test/java/                       # Unit and integration tests
└── pom.xml                              # Maven configuration
```

## API Endpoints

### 1. Register User
**Endpoint**: `POST /api/auth/register`

**Request**:
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePassword123"
}
```

**Response** (201 Created):
```json
{
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "message": "User registered successfully"
}
```

**Validation Rules**:
- Username: 3-50 characters, alphanumeric and underscores only
- Password: Minimum 6 characters
- Email: Valid email format

**Error Responses**:
- `400 Bad Request`: Invalid input or username/email already exists
- `500 Internal Server Error`: Database error

### 2. Login User
**Endpoint**: `POST /api/auth/login`

**Request**:
```json
{
  "username": "john_doe",
  "password": "SecurePassword123"
}
```

**Response** (200 OK):
```json
{
  "userId": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "USER",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "message": "Login successful"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid input
- `401 Unauthorized`: Invalid credentials or user not found
- `500 Internal Server Error`: Server error

### 3. Validate Token
**Endpoint**: `GET /api/auth/validate`

**Headers**:
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response** (200 OK - Valid Token):
```json
{
  "userId": 1,
  "username": "john_doe",
  "role": "USER",
  "valid": true,
  "message": "Token is valid"
}
```

**Response** (200 OK - Invalid Token):
```json
{
  "valid": false,
  "message": "Token validation failed: JWT signature does not match"
}
```

## JWT Token Structure

The JWT token contains the following claims:

```
Header:
{
  "alg": "HS512",
  "typ": "JWT"
}

Payload:
{
  "sub": "1",                    // User ID
  "username": "john_doe",        // Username
  "role": "USER",                // User role
  "email": "john@example.com",   // Email
  "iat": 1699564800,             // Issued at
  "exp": 1699568400               // Expiration (1 hour)
}

Signature:
HMACSHA512(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

**Token Expiration**: 1 hour (3600000 milliseconds)

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    enabled BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);
```

## Configuration

### application.yml (Bootstrap)
Located in `src/main/resources/application.yml`:
- Configures service name and Eureka registration
- Imports centralized configuration from Config Server

### auth-service.yml (Central Config)
Located in Config Server's `config-repo/auth-service.yml`:
- Database configuration (PostgreSQL connection)
- JWT settings (secret key, expiration)
- Management endpoints (actuator, health)
- Logging configuration

### JWT Configuration
```yaml
app:
  jwt:
    secret: "your-secret-key-very-secure-change-in-production"
    expiration: 3600000  # 1 hour
```

**Important**: Change the JWT secret in production to a secure, randomly generated value.

## Running the Service

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 12+
- Config Server running on `http://localhost:8888`
- Discovery Server running on `http://localhost:8761`

### Build
```bash
mvn clean package
```

### Run
```bash
mvn spring-boot:run
```

Or using the JAR:
```bash
java -jar target/auth-service-1.0.0.jar
```

### Docker
```bash
docker build -t auth-service:1.0.0 .
docker run -p 8084:8084 -e POSTGRES_HOST=postgres -e POSTGRES_PORT=5332 auth-service:1.0.0
```

## Integration with Other Services

The Auth Service integrates with other microservices via:

1. **Service Discovery**: Registers with Eureka Server for dynamic discovery
2. **API Gateway**: Receives requests through the API Gateway, which:
   - Routes requests to Auth Service
   - Extracts authentication header
   - Forwards role information to downstream services
3. **Common-Lib**: Uses shared exception handling and role extraction utilities

### Example: Using Auth Token in API Calls

```bash
# 1. Login to get token
TOKEN=$(curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"SecurePassword123"}' \
  | jq -r '.token')

# 2. Use token to call protected endpoints
curl -X GET http://localhost:8081/api/products \
  -H "Authorization: Bearer $TOKEN"
```

## Security Features

- **Stateless Authentication**: JWT tokens enable stateless authentication without server-side sessions
- **Password Security**: Passwords are hashed using BCrypt with salt
- **CORS Configuration**: Restricted to allowed origins
- **CSRF Protection**: Disabled for stateless REST APIs
- **Role-Based Access Control**: Support for multiple roles and fine-grained permissions

## Error Handling

All errors follow a standardized format via `GlobalExceptionHandler`:

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Username already exists",
  "path": "/api/auth/register"
}
```

Common HTTP Status Codes:
- `200 OK`: Successful operation
- `201 Created`: User successfully created
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Invalid credentials or missing token
- `500 Internal Server Error`: Server-side error

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### API Testing with cURL

**Register**:
```bash
curl -X POST http://localhost:8084/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"testuser",
    "email":"test@example.com",
    "password":"TestPass123"
  }'
```

**Login**:
```bash
curl -X POST http://localhost:8084/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username":"testuser",
    "password":"TestPass123"
  }'
```

**Validate Token**:
```bash
curl -X GET http://localhost:8084/api/auth/validate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Debugging

Enable debug logging:
```yaml
logging:
  level:
    com.ecommerce: DEBUG
```

Check service health:
```bash
curl http://localhost:8084/actuator/health
```

Access API documentation:
```
http://localhost:8084/swagger-ui.html
```
