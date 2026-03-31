# CRM Desktop Application

A robust CRM desktop application with a **Spring Boot 3** backend API and a **JavaFX 21** desktop GUI — using **Java 17**, **PostgreSQL**, **Redis**, JWT-based authentication, role-based access control, and RESTful APIs for managing customers, leads, tasks, and interactions.

> **📘 Full setup & deployment guide → [SETUP.md](SETUP.md)**

## Architecture

| Module | Description |
|--------|-------------|
| **`crm-backend/`** | Spring Boot 3 REST API — business logic, database, security, email, PDF |
| **`crm-desktop/`** | JavaFX 21 desktop GUI — login, sidebar navigation, customer/lead/task screens, dashboard, reports |

The desktop app communicates with the backend via REST API using Java's built-in `HttpClient`. JWT tokens are stored **in memory only** (never written to disk). The backend can also be containerized with Docker Compose.

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
| Backend Framework | Spring Boot 3.2.3 |
| Desktop GUI | JavaFX 21 |
| Security | Spring Security + JWT (JJWT 0.12.5) |
| Database | PostgreSQL |
| Cache | Redis |
| Migrations | Flyway |
| Email | JavaMailSender (SMTP + TLS) |
| PDF | iText 5.5.13.3 |
| Excel | Apache POI 5.2.5 |
| Audit | Spring AOP |
| API Docs | SpringDoc OpenAPI (Swagger) |
| JSON (Desktop) | Gson 2.10.1 |
| HTTP Client | Java HttpClient (built-in) |
| Testing | JUnit 5 + Mockito |
| Build | Maven |
| Container | Docker + Docker Compose |
| Packaging | jpackage (.exe installer) |

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
cp .env.example .env
# Edit .env with your settings (especially JWT_SECRET and DB_PASSWORD)

# Start all services
docker compose up -d

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

# 4. Build and run backend
mvn clean compile
mvn spring-boot:run

# Or run backend tests (91 tests)
mvn test
```

### Option 3: Run the Desktop App

```bash
# Requires: backend running at http://localhost:8080

cd crm-desktop
mvn clean compile
mvn javafx:run

# Or run desktop tests (40 tests)
mvn test

# Build Windows .exe installer (Windows only)
build-installer.bat
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
CRM-TempO-Project/
├── crm-backend/                        # Spring Boot REST API
│   ├── src/main/java/com/crm/
│   │   ├── config/                     # Security, JWT, Redis, AOP, Filters
│   │   ├── controller/                 # REST API endpoints (10 controllers)
│   │   ├── dto/request/                # Input DTOs (12)
│   │   ├── dto/response/               # Output DTOs (16)
│   │   ├── exception/                  # Custom exceptions + GlobalExceptionHandler
│   │   ├── model/entity/               # JPA entities (10)
│   │   ├── model/enums/                # Enumerations (6)
│   │   ├── repository/                 # Data access layer + specifications
│   │   ├── scheduler/                  # Cron jobs (overdue tasks, email reminders)
│   │   ├── service/                    # Business logic (12 services)
│   │   └── util/                       # InputSanitizer, CsvSanitizer, FileValidator
│   ├── src/main/resources/
│   │   ├── db/migration/               # Flyway migrations (V1-V10)
│   │   ├── application.yml
│   │   └── application-prod.yml
│   ├── src/test/java/                  # 91 unit tests (9 test classes)
│   ├── Dockerfile                      # Multi-stage Docker build
│   └── pom.xml
│
├── crm-desktop/                        # JavaFX 21 Desktop GUI
│   ├── src/main/java/com/crm/desktop/
│   │   ├── CrmDesktopApp.java          # Main entry point
│   │   ├── api/                        # HTTP API clients (9 classes)
│   │   │   ├── ApiClient.java          # JWT-aware HTTP wrapper
│   │   │   ├── AuthApi.java            # Login, register, refresh
│   │   │   ├── CustomerApi.java        # Customer CRUD
│   │   │   ├── LeadApi.java            # Lead pipeline
│   │   │   ├── TaskApi.java            # Task management
│   │   │   ├── InteractionApi.java     # Interaction logging
│   │   │   ├── ReportApi.java          # Dashboard & reports
│   │   │   ├── NotificationApi.java    # Notifications
│   │   │   └── DocumentApi.java        # File download
│   │   ├── controller/                 # FXML screen controllers (13)
│   │   │   ├── LoginController.java
│   │   │   ├── MainController.java     # Sidebar + nav
│   │   │   ├── CustomerListController.java
│   │   │   ├── CustomerDetailController.java
│   │   │   ├── CustomerFormController.java
│   │   │   ├── LeadPipelineController.java
│   │   │   ├── LeadDetailController.java
│   │   │   ├── TaskListController.java
│   │   │   ├── TaskFormController.java
│   │   │   ├── InteractionTimelineController.java
│   │   │   ├── DashboardController.java
│   │   │   ├── ReportController.java
│   │   │   └── ImportController.java
│   │   ├── model/                      # Client-side data models
│   │   ├── service/
│   │   │   ├── SessionManager.java     # In-memory JWT + 15min idle timeout
│   │   │   ├── NotificationPoller.java # Background poll every 30s
│   │   │   └── DraftSaver.java         # Auto-save form drafts
│   │   └── util/
│   │       ├── Validator.java          # Client-side input validation
│   │       ├── Formatter.java          # Currency, date, duration formatting
│   │       └── AlertHelper.java        # Reusable dialogs
│   ├── src/main/resources/
│   │   ├── fxml/                       # FXML layouts (13 screens)
│   │   ├── css/styles.css              # Application theme
│   │   └── images/                     # Icons, splash
│   ├── src/test/java/                  # 40 unit tests (4 test classes)
│   ├── build-installer.bat             # jpackage .exe builder
│   └── pom.xml
│
├── .github/workflows/ci.yml           # GitHub Actions CI pipeline
├── docker-compose.yml                  # App + PostgreSQL + Redis
├── .env.example                        # Environment template for Docker
├── SETUP.md                            # Step-by-step deployment guide
├── CRM-Project-Phases.md               # Project blueprint (9 phases)
└── README.md
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
# Backend tests (91 tests)
cd crm-backend
mvn test

# Desktop tests (40 tests)
cd crm-desktop
mvn test
```

### Backend Tests cover:
- **AuthService** (10 tests): Registration, login, lockout, JWT refresh
- **CustomerService** (14 tests): CRUD, role-based access, XSS sanitization, optimistic locking
- **LeadService** (11 tests): Stage transitions, validation rules, access control
- **TaskService** (7 tests): Creation, completion, overdue detection
- **InteractionService** (8 tests): 24h edit window, rate limiting, immutability
- **NotificationService** (5 tests): Creation, read marking, access control
- **InputSanitizer** (10 tests): HTML stripping, XSS prevention
- **CsvSanitizer** (12 tests): Formula injection prevention
- **FileValidator** (14 tests): Magic-byte validation, path traversal, size limits

### Desktop Tests cover:
- **SessionManager** (10 tests): Token storage, idle timeout, role checking
- **Validator** (13 tests): Email, phone, name, password, decimal validation
- **Formatter** (9 tests): Date, currency, number, duration formatting
- **DraftSaver** (8 tests): Draft save, load, clear, lifecycle

## License

This project is for academic/educational purposes.
