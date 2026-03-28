# HRPilot Backend

Spring Boot 3.5 REST API with JWT authentication and role-based access control.

## Module Structure

```
src/main/java/com/hrpilot/backend/
├── auth/           # Login & register endpoints
├── user/           # User CRUD (ADMIN only)
├── employee/       # Employee management
├── department/     # Department management
├── leave/          # Leave request workflow
├── payroll/        # Payroll records
├── config/         # Security, JWT, OpenAPI
├── common/         # BaseEntity, exceptions, global error handler
└── health/         # Health check
```

Each module follows the pattern: `Entity` + `Repository` + `Service` + `Controller` + `DTOs`

## Custom Exception Hierarchy

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | Entity not found by ID |
| `DuplicateResourceException` | 409 | Unique constraint violation |
| `BusinessRuleException` | 422 | Business logic violation |
| `AuthenticationException` | 401 | Invalid credentials |

All handled by `GlobalExceptionHandler` with consistent `ErrorResponse` format.

## Security

- JWT tokens with configurable expiration
- `JwtAuthenticationFilter` validates tokens and loads roles from DB
- `@PreAuthorize` annotations on controller methods for RBAC
- Inactive accounts are blocked at filter level

## Running

```bash
# With dev profile (default)
mvn spring-boot:run

# Run tests
mvn test

# Build JAR
mvn package -DskipTests
```

## Environment Variables (prod profile)

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC connection string |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret (min 256-bit) |
