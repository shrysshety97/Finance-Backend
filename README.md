# Finance Data Processing & Access Control Backend

A production-quality RESTful backend for a Finance Dashboard system — built with **Java 8**, **Spring Boot 2.7.18**, **Spring Security + JWT**, **MySQL**, and **Hibernate/JPA**.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Architecture Overview](#architecture-overview)
3. [Project Structure](#project-structure)
4. [Role-Based Access Control](#role-based-access-control)
5. [API Reference](#api-reference)
6. [Data Model](#data-model)
7. [Setup & Running Locally](#setup--running-locally)
8. [Running Tests](#running-tests)
9. [Key Design Decisions & Assumptions](#key-design-decisions--assumptions)
10. [Sample cURL Workflow](#sample-curl-workflow)

---

## Tech Stack

| Layer            | Technology                              |
|------------------|-----------------------------------------|
| Language         | Java 8                                  |
| Framework        | Spring Boot 2.7.18                      |
| Web              | Spring MVC (REST)                       |
| Security         | Spring Security + JWT (JJWT 0.12)       |
| Persistence      | Spring Data JPA + Hibernate             |
| Database         | MySQL 8 (H2 for tests)                  |
| Validation       | Java Bean Validation                    |
| Build            | Maven                                   |
| Boilerplate      | Lombok                                  |
| Testing          | JUnit 5, Mockito, MockMvc               |

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                        HTTP Client                           │
└──────────────────────────┬───────────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │  JwtAuthenticationFilter│  ← validates Bearer token
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │  Spring Security        │  ← role enforcement (@PreAuthorize)
              └────────────┬────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼──────┐  ┌────────▼──────┐  ┌───────▼──────────┐
│AuthController│  │RecordController│  │DashboardController│
└───────┬──────┘  └────────┬──────┘  └───────┬──────────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼──────┐  ┌────────▼──────┐  ┌───────▼─────────┐
│  AuthService │  │ RecordService  │  │DashboardService  │
└───────┬──────┘  └────────┬──────┘  └───────┬─────────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           │
               ┌───────────▼───────────┐
               │  Spring Data JPA      │
               │  (Repositories)       │
               └───────────┬───────────┘
                           │
               ┌───────────▼───────────┐
               │       MySQL 8         │
               └───────────────────────┘
```

### Security Flow

```
Request → JwtAuthenticationFilter
            ├── Extract "Authorization: Bearer <token>"
            ├── JwtTokenProvider.validateToken()
            ├── Load UserDetails (CustomUserDetailsService)
            └── Set Authentication in SecurityContextHolder
                        │
                        ▼
              @PreAuthorize checks role
```

---

## Project Structure

```
src/main/java/com/finance/
├── FinanceApplication.java          # Entry point
├── config/
│   ├── DataInitializer.java         # Seeds default users on startup
│   ├── JpaAuditingConfig.java       # Enables @CreatedDate / @LastModifiedDate
│   └── SecurityConfig.java          # Spring Security + JWT setup
├── controller/
│   ├── AuthController.java          # /api/auth/**  (public)
│   ├── DashboardController.java     # /api/dashboard/**
│   ├── FinancialRecordController.java # /api/records/**
│   └── UserController.java          # /api/users/**
├── dto/
│   ├── request/                     # Inbound validated DTOs
│   └── response/                    # Outbound response DTOs
├── entity/
│   ├── FinancialRecord.java         # Soft-deletable financial entry
│   └── User.java                    # User with role + status
├── enums/
│   ├── RecordType.java              # INCOME | EXPENSE
│   ├── Role.java                    # VIEWER | ANALYST | ADMIN
│   └── UserStatus.java              # ACTIVE | INACTIVE
├── exception/
│   ├── BadRequestException.java
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
├── repository/
│   ├── FinancialRecordRepository.java  # Custom JPQL + native queries
│   └── UserRepository.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationEntryPoint.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtTokenProvider.java
└── service/
    ├── AuthService.java
    ├── DashboardService.java
    ├── FinancialRecordService.java
    └── UserService.java
```

---

## Role-Based Access Control

| Endpoint                             | VIEWER | ANALYST | ADMIN |
|--------------------------------------|:------:|:-------:|:-----:|
| `POST /api/auth/register`            | ✅ public | ✅ | ✅ |
| `POST /api/auth/login`               | ✅ public | ✅ | ✅ |
| `GET /api/users/me`                  | ✅     | ✅      | ✅    |
| `GET /api/users`                     | ❌     | ❌      | ✅    |
| `GET /api/users/{id}`                | ❌     | ❌      | ✅    |
| `PATCH /api/users/{id}`              | ❌     | ❌      | ✅    |
| `PATCH /api/users/{id}/status`       | ❌     | ❌      | ✅    |
| `DELETE /api/users/{id}`             | ❌     | ❌      | ✅    |
| `GET /api/records`                   | ❌     | ✅      | ✅    |
| `GET /api/records/{id}`              | ❌     | ✅      | ✅    |
| `POST /api/records`                  | ❌     | ❌      | ✅    |
| `PUT /api/records/{id}`              | ❌     | ❌      | ✅    |
| `DELETE /api/records/{id}`           | ❌     | ❌      | ✅    |
| `GET /api/dashboard/summary`         | ✅     | ✅      | ✅    |
| `GET /api/dashboard/category-totals` | ✅     | ✅      | ✅    |
| `GET /api/dashboard/monthly-trends`  | ❌     | ✅      | ✅    |
| `GET /api/dashboard/weekly-trends`   | ❌     | ✅      | ✅    |
| `GET /api/dashboard/recent-activity` | ❌     | ✅      | ✅    |
| `GET /api/records/search`            | ❌     | ✅      | ✅    |

---

## API Reference

All responses follow this envelope:

```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "timestamp": "2024-01-15T10:30:00"
}
```

Validation errors include a `data` map of `{ "fieldName": "error message" }`.

---

### Auth

#### `POST /api/auth/register`
Register a new user and receive a JWT token.

**Request body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "secret123",
  "role": "ANALYST"
}
```

**Response `201`:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": 2,
    "username": "john_doe",
    "email": "john@example.com",
    "role": "ANALYST"
  }
}
```

---

#### `POST /api/auth/login`
Authenticate and receive a JWT token.

**Request body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response `200`:** Same shape as register response.

---

### Users *(ADMIN only except `/me`)*

#### `GET /api/users?page=0&size=10&status=ACTIVE`
List all users with optional status filter and pagination.

#### `GET /api/users/{id}`
Get a specific user by ID.

#### `GET /api/users/me`
Get the currently authenticated user's profile (any role).

#### `PUT /api/users/{id}`
Partial update — only provided fields are changed.

**Request body (all fields optional):**
```json
{
  "username": "new_username",
  "email": "new@email.com",
  "password": "newpassword",
  "role": "VIEWER",
  "status": "INACTIVE"
}
```

#### `PATCH /api/users/{id}/status?status=INACTIVE`
Toggle a user's active/inactive status.

#### `DELETE /api/users/{id}`
Hard delete a user record.

---

### Financial Records

#### `GET /api/records`
List records with optional filters and pagination.

**Query params:**
| Param       | Type       | Example                |
|-------------|------------|------------------------|
| `type`      | RecordType | `INCOME` or `EXPENSE`  |
| `category`  | string     | `Salary`               |
| `startDate` | ISO date   | `2024-01-01`           |
| `endDate`   | ISO date   | `2024-12-31`           |
| `page`      | int        | `0`                    |
| `size`      | int        | `10`                   |

**Response `200`:**
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 10,
    "totalElements": 42,
    "totalPages": 5,
    "first": true,
    "last": false
  }
}
```

#### `GET /api/records/{id}`
Get a single record by ID.

#### `POST /api/records` *(ADMIN only)*
Create a new financial record.

**Request body:**
```json
{
  "amount": 50000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2024-01-15",
  "notes": "January salary payment"
}
```

#### `PUT /api/records/{id}` *(ADMIN only)*
Full update of an existing record (same body as POST).

#### `DELETE /api/records/{id}` *(ADMIN only)*
Soft-delete the record (hidden from queries, retained in DB for auditing).

---

### Dashboard

#### `GET /api/dashboard/summary`
```json
{
  "data": {
    "totalIncome": 150000.00,
    "totalExpenses": 95000.00,
    "netBalance": 55000.00,
    "totalRecords": 48
  }
}
```

#### `GET /api/dashboard/category-totals`
```json
{
  "data": [
    { "category": "Salary",  "type": "INCOME",  "total": 120000.00 },
    { "category": "Rent",    "type": "EXPENSE", "total": 36000.00  }
  ]
}
```

#### `GET /api/dashboard/monthly-trends?months=6`
```json
{
  "data": [
    { "year": 2024, "month": 1, "monthName": "JANUARY", "type": "INCOME",  "total": 50000.00 },
    { "year": 2024, "month": 1, "monthName": "JANUARY", "type": "EXPENSE", "total": 32000.00 }
  ]
}
```

#### `GET /api/dashboard/recent-activity?limit=5`
Returns the 5 most recently created records (max 50).

---

## Data Model

### `users` table

| Column       | Type         | Notes                          |
|--------------|--------------|--------------------------------|
| `id`         | BIGINT PK    | Auto-increment                 |
| `username`   | VARCHAR(50)  | Unique, 3-50 chars             |
| `email`      | VARCHAR(100) | Unique, valid email            |
| `password`   | VARCHAR(255) | BCrypt hashed                  |
| `role`       | ENUM         | VIEWER / ANALYST / ADMIN       |
| `status`     | ENUM         | ACTIVE / INACTIVE              |
| `created_at` | DATETIME     | Auto-set by JPA Auditing       |
| `updated_at` | DATETIME     | Auto-set by JPA Auditing       |

### `financial_records` table

| Column       | Type           | Notes                          |
|--------------|----------------|--------------------------------|
| `id`         | BIGINT PK      | Auto-increment                 |
| `amount`     | DECIMAL(15,2)  | Must be > 0                    |
| `type`       | ENUM           | INCOME / EXPENSE               |
| `category`   | VARCHAR(100)   | e.g. "Salary", "Rent"          |
| `date`       | DATE           | Must not be in the future      |
| `notes`      | TEXT           | Optional, max 1000 chars       |
| `deleted`    | BOOLEAN        | Soft-delete flag               |
| `created_by` | FK → users(id) | Nullable (user may be deleted) |
| `created_at` | DATETIME       | Auto-set by JPA Auditing       |
| `updated_at` | DATETIME       | Auto-set by JPA Auditing       |

---

## Setup & Running Locally

### Prerequisites

- Java 8+
- Maven 3.8+
- MySQL 8.0+

### 1. Clone the repository

```bash
git clone <repository-url>
cd finance-backend
```

### 2. Create the MySQL database

```sql
CREATE DATABASE finance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure database credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 4. (Optional) Change the JWT secret

Generate a secure Base64-encoded 256-bit key:
```bash
openssl rand -base64 64
```
Paste the output into `app.jwt.secret` in `application.properties`.

### 5. Build and run

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

The server starts on **http://localhost:8080**

### Swagger UI API Documentation

Once the server is running, the interactive Swagger UI API documentation is available at:
**http://localhost:8080/swagger-ui/index.html**

OpenAPI v3 JSON specification is available at:
**http://localhost:8080/v3/api-docs**

### Default seeded users (created automatically on first run)

| Username | Password   | Role    |
|----------|------------|---------|
| admin    | admin123   | ADMIN   |
| analyst  | analyst123 | ANALYST |
| viewer   | viewer123  | VIEWER  |

> ⚠️ **Change these passwords immediately after your first login in production.**

---

## Running Tests

```bash
# Run all tests (uses H2 in-memory database automatically)
mvn test

# Run a specific test class
mvn test -Dtest=AuthServiceTest

# Run with coverage report
mvn test jacoco:report
```

Test coverage includes:
- **Unit tests**: `AuthServiceTest`, `FinancialRecordServiceTest`, `DashboardServiceTest`, `JwtTokenProviderTest`
- **Integration tests**: `AuthControllerIntegrationTest`, `FinancialRecordControllerIntegrationTest`

---

## Key Design Decisions & Assumptions

### 1. Soft Delete
Financial records are never hard-deleted — a `deleted` boolean flag is used instead. This preserves historical audit trails (important in finance) while hiding records from normal queries. The `findByIdAndDeletedFalse()` repository method enforces this transparently.

### 2. Stateless JWT Authentication
No server-side sessions are maintained. Every request must carry a valid `Authorization: Bearer <token>` header. This makes the API horizontally scalable with no sticky sessions needed.

### 3. Role Enforcement via `@PreAuthorize`
Instead of configuring every route in `SecurityFilterChain` (which becomes hard to maintain), role checks are declared directly on controller methods using `@PreAuthorize`. This co-locates the access policy with the endpoint it protects, making it easy to audit.

### 4. Inactive Users Cannot Log In
`CustomUserDetailsService` maps `UserStatus.INACTIVE` to Spring Security's `disabled=true`. Spring then throws `DisabledException` on login, which the `GlobalExceptionHandler` maps to a `403` with a clear message.

### 5. BigDecimal for Money
All monetary amounts use `BigDecimal` (not `double`/`float`) to avoid floating-point rounding errors — a critical correctness requirement for financial systems.

### 6. Paginated API Design
All list endpoints accept `page` and `size` query parameters and return a `PagedResponse<T>` with metadata (`totalElements`, `totalPages`, `first`, `last`). This prevents unbounded queries on large datasets.

### 7. Standardised `ApiResponse<T>` Envelope
Every endpoint returns the same `{ success, message, data, timestamp }` shape. This makes client-side error handling predictable and testable.

### 8. First Admin via `/api/auth/register`
The register endpoint is public to allow bootstrapping the first admin user without a chicken-and-egg problem. In a production system this endpoint would be behind an additional secret or removed after initial setup. The `DataInitializer` seeds default users to further simplify first-run setup.

### 9. Assumptions on Role Model
- **VIEWER**: Can see aggregated dashboard data (summary + category totals) but cannot see individual records or sensitive trends.
- **ANALYST**: Can view individual records and all analytics but cannot create/modify/delete data.
- **ADMIN**: Full system access — user management, record management, all analytics.

### 10. JPA Schema Management
`spring.jpa.hibernate.ddl-auto=update` is used for development convenience. For a production deployment this should be switched to `validate` with Flyway or Liquibase managing schema migrations.

---

## Sample cURL Workflow

```bash
BASE=http://localhost:8080

# 1. Login as admin
TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.accessToken')

# 2. Create a financial record
curl -X POST $BASE/api/records \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 75000.00,
    "type": "INCOME",
    "category": "Salary",
    "date": "2024-01-15",
    "notes": "Monthly salary"
  }'

# 3. Create an expense
curl -X POST $BASE/api/records \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 15000.00,
    "type": "EXPENSE",
    "category": "Rent",
    "date": "2024-01-01"
  }'

# 4. Get dashboard summary (works for all roles)
curl $BASE/api/dashboard/summary \
  -H "Authorization: Bearer $TOKEN"

# 5. Get category totals
curl $BASE/api/dashboard/category-totals \
  -H "Authorization: Bearer $TOKEN"

# 6. Get monthly trends (last 6 months)
curl "$BASE/api/dashboard/monthly-trends?months=6" \
  -H "Authorization: Bearer $TOKEN"

# 7. Filter records by type and date range
curl "$BASE/api/records?type=INCOME&startDate=2024-01-01&endDate=2024-12-31&page=0&size=5" \
  -H "Authorization: Bearer $TOKEN"

# 8. Create an analyst user
curl -X POST $BASE/api/auth/register \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_analyst",
    "email": "jane@company.com",
    "password": "secure456",
    "role": "ANALYST"
  }'

# 9. Deactivate a user (id=3)
curl -X PATCH "$BASE/api/users/3/status?status=INACTIVE" \
  -H "Authorization: Bearer $TOKEN"
```
