# CRM Desktop Application — Backend API

A robust CRM desktop application backend using **Java 17**, **Spring Boot 3**, **PostgreSQL**, **Redis**, with JWT-based authentication, role-based access control, and RESTful APIs for managing customers, leads, tasks, and interactions.

## Features

| Module | Description |
|--------|-------------|
| **Authentication** | Register, Login, JWT access + refresh tokens, Account lockout (5 attempts → 30min lock), Rate limiting |
| **Customer Management** | CRUD with role-based filtering, PII masking, XSS prevention, Soft delete, Optimistic locking |
| **Lead Pipeline** | Kanban stages (New → Contacted → Qualified → Proposal → Won/Lost), Stage transition rules, BigDecimal money |
| **Task Management** | Create, assign, track tasks with priorities, Auto-mark overdue (hourly scheduler) |
| **Interaction Logging** | Immutable call/email/meeting records, 24h edit window, No delete endpoint |
| **Dashboard & Reports** | KPI metrics, Conversion rates, Sales by rep, Monthly trends, Activity summary |
| **PDF Export** | iText PDF generation with watermarks, Streamed directly (never saved to disk) |
| **Email Notifications** | Async with 3x retry, Daily task reminders (8 AM), Overdue alerts (9 AM), No PII in emails |
| **File Upload** | Magic-byte validation (not extension), 10MB max, UUID stored names, Path traversal prevention |
| **Excel/CSV Import** | Apache POI, 5000 row max, Batch 100/commit, CSV injection sanitization |
| **Audit Trail** | Spring AOP auto-capture, Immutable log, Never logs passwords |
| **Notifications** | In-app notifications with unread count, Auto-created on task/lead/customer assignments |
| **Security Hardening** | HSTS, X-Frame-Options: DENY, CSP, nosniff, Request logging (no bodies) |

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Security | Spring Security + JWT (JJWT 0.12.5) |
| Database | PostgreSQL |
| Cache | Redis |
| Migrations | Flyway |
| Email | JavaMailSender (SMTP + TLS) |
| PDF | iText 5.5.13.3 |
| Excel | Apache POI 5.2.5 |
| Audit | Spring AOP |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Testing | JUnit 5 + Mockito |
| Build | Maven |
| Container | Docker + Docker Compose |

## Roles & Access Control

| Role | Access |
|------|--------|
| **ADMIN** | Full access, sees all data, deletes records, views all reports |
| **MANAGER** | Team data, assigns work, team reports, cannot see other teams |
| **SALES_REP** | Own assigned data only, logs interactions, moves leads |

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 16+
- Redis 7+ (optional for caching)
- Docker & Docker Compose (for containerized deployment)

## Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repository
git clone https://github.com/azharali0/CRM-TempO-Project.git
cd CRM-TempO-Project

# Create environment file
cp crm-backend/.env.example .env
# Edit .env with your settings (especially JWT_SECRET and DB_PASSWORD)

# Start all services
docker-compose up -d

# The API will be available at http://localhost:8080
# Swagger docs at http://localhost:8080/swagger-ui.html
```

### Option 2: Local Development

```bash
# 1. Start PostgreSQL and Redis locally
# 2. Create database
createdb crm_db

# 3. Configure environment
cd crm-backend
cp .env.example .env
# Edit .env with your local settings

# 4. Build and run
mvn clean compile
mvn spring-boot:run

# Or run tests
mvn test
```

## API Endpoints

### Authentication
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login | No |
| POST | `/api/auth/refresh` | Refresh access token | No |
| POST | `/api/auth/logout` | Logout (revoke refresh token) | Yes |

### Customers
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/customers` | List (paginated, filtered, role-based) | Yes |
| POST | `/api/customers` | Create customer | Yes |
| GET | `/api/customers/{id}` | Get customer detail | Yes |
| PUT | `/api/customers/{id}` | Update customer | Yes |
| DELETE | `/api/customers/{id}` | Soft-delete (ADMIN only) | Yes |
| POST | `/api/customers/import` | Import from Excel/CSV | Yes |
| POST | `/api/customers/{id}/documents` | Upload document | Yes |
| GET | `/api/customers/{id}/documents` | List documents | Yes |

### Leads
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/leads` | List leads | Yes |
| POST | `/api/leads` | Create lead | Yes |
| GET | `/api/leads/{id}` | Get lead detail | Yes |
| PUT | `/api/leads/{id}` | Update lead | Yes |
| PATCH | `/api/leads/{id}/stage` | Change lead stage | Yes |

### Tasks
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/tasks` | List all tasks (MANAGER/ADMIN) | Yes |
| GET | `/api/tasks/my` | List my tasks | Yes |
| POST | `/api/tasks` | Create task | Yes |
| GET | `/api/tasks/{id}` | Get task detail | Yes |
| PUT | `/api/tasks/{id}` | Update task | Yes |
| PATCH | `/api/tasks/{id}/complete` | Mark as complete | Yes |

### Interactions
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/interactions` | Log interaction | Yes |
| GET | `/api/interactions/customer/{id}` | Customer timeline | Yes |
| GET | `/api/interactions/recent` | Recent interactions | Yes |
| PUT | `/api/interactions/{id}` | Edit (24h window) | Yes |

### Reports
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/reports/dashboard` | Dashboard KPIs | Yes |
| GET | `/api/reports/conversion` | Conversion report | Yes |
| GET | `/api/reports/sales-by-rep` | Sales by rep (MANAGER/ADMIN) | Yes |
| GET | `/api/reports/monthly-trend` | Monthly trends | Yes |
| GET | `/api/reports/activity-summary` | Activity summary | Yes |
| GET | `/api/reports/customers/pdf` | Customer report PDF | Yes |
| GET | `/api/reports/leads/pdf` | Leads report PDF | Yes |

### Notifications
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/notifications/my` | My notifications | Yes |
| GET | `/api/notifications/unread-count` | Unread count | Yes |
| PATCH | `/api/notifications/{id}/read` | Mark as read | Yes |

### Audit Log
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/audit-log` | Query audit logs (MANAGER/ADMIN) | Yes |

### Documents
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/documents/{id}/download` | Download document | Yes |
| DELETE | `/api/documents/{id}` | Soft-delete document | Yes |

### System
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/system/version` | App version | No |
| GET | `/api/system/health` | Health check | No |

## Security Features

- **JWT Authentication**: 15-minute access tokens + 7-day refresh tokens
- **Account Lockout**: 5 failed attempts → 30-minute lock
- **Rate Limiting**: 5 requests/minute/IP on auth endpoints
- **BCrypt** password hashing (strength 12)
- **Input Sanitization**: HTML tag stripping on all text fields
- **CSV Injection Prevention**: Formula prefix stripping on imports
- **Magic-Byte File Validation**: Validates file content, not just extension
- **Path Traversal Prevention**: Stripped from all filenames
- **Immutable Audit Log**: Every action tracked, never modifiable
- **Security Headers**: HSTS, X-Content-Type-Options: nosniff, X-Frame-Options: DENY, CSP
- **Role-Based Row Filtering**: Server-side on every query
- **Optimistic Locking**: Prevents concurrent update conflicts
- **PII Masking**: Phone numbers masked in list views

## Project Structure

```
crm-backend/
├── src/main/java/com/crm/
│   ├── config/          # Security, JWT, AOP, Filters
│   ├── controller/      # REST API endpoints
│   ├── dto/
│   │   ├── request/     # Input DTOs
│   │   └── response/    # Output DTOs
│   ├── exception/       # Custom exceptions
│   ├── model/
│   │   ├── entity/      # JPA entities
│   │   └── enums/       # Enumerations
│   ├── repository/      # Data access layer
│   ├── scheduler/       # Cron jobs
│   ├── service/         # Business logic
│   └── util/            # Utilities
├── src/main/resources/
│   ├── db/migration/    # Flyway migrations (V1-V10)
│   ├── application.yml
│   └── application-prod.yml
├── src/test/java/       # Unit tests
├── Dockerfile
├── pom.xml
└── .env.example
```

## Database Migrations

| Migration | Description |
|-----------|-------------|
| V1 | Users table with account lockout |
| V2 | Customers table with soft delete |
| V3 | Leads table with pipeline stages |
| V4 | Tasks table with priority and status |
| V5 | Interactions table (immutable) |
| V6 | Documents table for file attachments |
| V7 | Notifications table |
| V8 | Audit log table (immutable) |
| V9 | Email log table |
| V10 | Refresh tokens table |

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/crm_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | JWT signing key (256-bit minimum) | — |
| `SERVER_PORT` | Application port | `8080` |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |
| `MAIL_USERNAME` | SMTP username | — |
| `MAIL_PASSWORD` | SMTP password | — |
| `FILE_UPLOAD_DIR` | File upload directory | `./uploads` |

## Running Tests

```bash
cd crm-backend
mvn test
```

Tests cover:
- **AuthService**: Registration, login, lockout, JWT refresh
- **CustomerService**: CRUD, role-based access, XSS sanitization, optimistic locking
- **LeadService**: Stage transitions, validation rules, access control
- **TaskService**: Creation, completion, overdue detection
- **InteractionService**: 24h edit window, rate limiting, immutability
- **NotificationService**: Creation, read marking, access control
- **InputSanitizer**: HTML stripping, XSS prevention
- **CsvSanitizer**: Formula injection prevention
- **FileValidator**: Magic-byte validation, path traversal, size limits

## License

This project is for academic/educational purposes.
