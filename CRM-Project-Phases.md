# CRM Desktop Application — Complete Project Blueprint

## Project Title
**Customer Relationship Management (CRM) Desktop Application**

## Project Statement
Developed a robust CRM desktop application using Java 17, Spring Boot 3 (backend), and JavaFX 21 (frontend GUI) with JWT-based authentication, role-based access control, and RESTful APIs for managing customers, leads, tasks, and interactions. Integrated PostgreSQL for persistence, Redis for caching, and JavaMailSender for automated notifications — packaged as a Windows `.exe` installer via jpackage.

---

## What Is This Project?

A **desktop application** (NOT a website) that a company installs on their staff's computers. The staff — salespersons, managers, and admins — use it daily to:

- **Remember every customer** — name, phone, email, company, purchase history
- **Never miss a follow-up** — tasks with due dates and automatic email reminders
- **Track sales deals** — lead pipeline from first contact to deal won/lost
- **See business performance** — dashboard with charts, reports, PDF exports
- **Stay accountable** — every action is logged, every change is tracked

---

## Who Uses It?

| Role | What They Do in the App |
|---|---|
| **Admin** | Full access. Manages users, sees everything, deletes records, views all reports |
| **Manager** | Sees their team's customers/leads/tasks. Assigns work. Views team reports. Cannot see other teams. |
| **Sales Rep** | Sees only their own assigned customers and tasks. Logs interactions. Moves leads through pipeline. |

---

## Full Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| Language | Java 17+ | Core programming language |
| Backend Framework | Spring Boot 3 | REST API, business logic, scheduling |
| Desktop GUI | JavaFX 21 | Windows desktop application (screens, forms, charts) |
| Security | Spring Security + JWT | Authentication, authorization, role-based access |
| Database | PostgreSQL | Primary data storage |
| Caching | Redis | Dashboard caching, session management |
| DB Migrations | Flyway | Versioned, repeatable database schema changes |
| Email | JavaMailSender (SMTP + TLS) | Automated notifications and reminders |
| PDF Generation | iText / JasperReports | Downloadable sales reports |
| Excel Import | Apache POI / OpenCSV | Bulk customer import from spreadsheets |
| HTTP Client | Java HttpClient / Retrofit | JavaFX app communicates with Spring Boot API |
| Logging | SLF4J + Logback | Structured application and security logging |
| API Documentation | SpringDoc OpenAPI (Swagger) | Auto-generated REST API docs |
| Resilience | Resilience4j | Circuit breakers for external services |
| Testing | JUnit 5 + Mockito + Testcontainers | Unit, integration, and security testing |
| Packaging | jpackage (JDK built-in) | Creates `.exe` Windows installer |
| Containerization | Docker + Docker Compose | Runs backend + PostgreSQL + Redis |
| Version Control | Git + GitHub | Source code management |

---

## Core Modules

| Module | Features |
|---|---|
| Authentication | Register, Login, JWT access + refresh tokens, Role-based access (Admin, Manager, Sales Rep), Account lockout |
| Customer Management | Add, Edit, Soft-Delete, Search, Filter, Pagination, Customer history timeline |
| Lead Pipeline | Kanban pipeline (New → Contacted → Qualified → Proposal → Won/Lost), stage transition rules |
| Task Management | Create tasks, assign to reps, set due dates and priority, auto-mark overdue |
| Interaction Logging | Log calls, emails, meetings per customer — immutable, append-only records |
| Dashboard | KPI cards, bar/pie/line charts, real-time metrics |
| Reports & PDF Export | Monthly sales, conversion rates, rep performance, downloadable PDF reports |
| File Management | Attach documents to customers, bulk import via Excel/CSV |
| Notifications | In-app bell notifications, system tray popups, email reminders |
| Audit Trail | Immutable log of every create, update, delete — who, what, when, old value, new value |

---

## Database Schema

```sql
-- USERS
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,           -- BCrypt hashed, NEVER plain text
    role VARCHAR(20) NOT NULL DEFAULT 'SALES_REP',  -- ADMIN, MANAGER, SALES_REP
    enabled BOOLEAN DEFAULT TRUE,
    failed_login_attempts INT DEFAULT 0,
    account_locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- CUSTOMERS
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(20),
    company VARCHAR(150),
    address TEXT,
    city VARCHAR(100),
    status VARCHAR(30) DEFAULT 'ACTIVE',     -- ACTIVE, INACTIVE, ARCHIVED
    assigned_to UUID REFERENCES users(id),
    created_by UUID REFERENCES users(id),
    last_contacted_at TIMESTAMP,
    version INT DEFAULT 0,                    -- Optimistic locking
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- LEADS
CREATE TABLE leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    title VARCHAR(200) NOT NULL,
    stage VARCHAR(30) DEFAULT 'NEW',         -- NEW, CONTACTED, QUALIFIED, PROPOSAL, WON, LOST
    value DECIMAL(12,2),                      -- BigDecimal — NEVER use double/float
    expected_close_date DATE,
    probability INT DEFAULT 0,                -- 0-100%
    lost_reason TEXT,
    owner_id UUID REFERENCES users(id),
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- TASKS
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    due_date TIMESTAMP NOT NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM',   -- LOW, MEDIUM, HIGH, URGENT
    status VARCHAR(20) DEFAULT 'PENDING',    -- PENDING, IN_PROGRESS, DONE, OVERDUE
    customer_id UUID REFERENCES customers(id),
    assigned_to UUID REFERENCES users(id),
    created_by UUID REFERENCES users(id),
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- INTERACTIONS (Immutable — no update/delete allowed after 24 hours)
CREATE TABLE interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    type VARCHAR(30) NOT NULL,               -- CALL, EMAIL, MEETING, NOTE
    subject VARCHAR(200),
    notes TEXT,
    duration INT,                             -- minutes (for calls/meetings)
    logged_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()        -- Set server-side only
);

-- DOCUMENTS
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    original_filename VARCHAR(255),
    stored_path VARCHAR(500),                 -- UUID-based path, NOT user-provided name
    file_size BIGINT,
    mime_type VARCHAR(100),
    deleted BOOLEAN DEFAULT FALSE,            -- Soft delete
    uploaded_by UUID REFERENCES users(id),
    uploaded_at TIMESTAMP DEFAULT NOW()
);

-- NOTIFICATIONS
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID REFERENCES users(id),
    title VARCHAR(200),
    message TEXT,
    type VARCHAR(50),                         -- TASK_ASSIGNED, TASK_OVERDUE, LEAD_STAGE_CHANGED
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- AUDIT LOG (Immutable — NO update or delete endpoints)
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,         -- CUSTOMER, LEAD, TASK
    entity_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,              -- CREATE, UPDATE, DELETE, STAGE_CHANGE
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    performed_by UUID REFERENCES users(id),
    performed_at TIMESTAMP DEFAULT NOW(),
    ip_address VARCHAR(45)
);

-- EMAIL LOG (for debugging delivery issues)
CREATE TABLE email_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email VARCHAR(150),
    subject VARCHAR(200),
    status VARCHAR(20),                       -- SENT, FAILED, RETRYING
    error_message TEXT,
    sent_at TIMESTAMP DEFAULT NOW()
);

-- REFRESH TOKENS (for JWT refresh token management)
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## API Endpoints

```
AUTH
  POST   /api/auth/register              → Register new user
  POST   /api/auth/login                 → Login, receive JWT access + refresh tokens
  POST   /api/auth/refresh               → Refresh expired access token
  POST   /api/auth/logout                → Revoke refresh token

CUSTOMERS
  GET    /api/customers                  → List (paginated, filtered by role)
  POST   /api/customers                  → Create
  GET    /api/customers/{id}             → Detail (full profile + history)
  PUT    /api/customers/{id}             → Update
  DELETE /api/customers/{id}             → Soft-delete (ADMIN only)
  POST   /api/customers/import           → Bulk import from Excel/CSV
  POST   /api/customers/{id}/documents   → Upload document
  GET    /api/customers/{id}/documents   → List documents

LEADS
  GET    /api/leads                      → List (filtered by role)
  POST   /api/leads                      → Create
  GET    /api/leads/{id}                 → Detail
  PUT    /api/leads/{id}                 → Update
  PATCH  /api/leads/{id}/stage           → Move to next stage (validated)

TASKS
  GET    /api/tasks/my                   → My tasks (assigned to current user)
  GET    /api/tasks                      → All tasks (MANAGER/ADMIN)
  POST   /api/tasks                      → Create
  PUT    /api/tasks/{id}                 → Update
  PATCH  /api/tasks/{id}/complete        → Mark as done

INTERACTIONS
  POST   /api/interactions               → Log new interaction
  GET    /api/interactions/customer/{id}  → Customer interaction timeline
  GET    /api/interactions/recent         → Recent activity feed
  PUT    /api/interactions/{id}          → Edit (within 24 hours only)

REPORTS
  GET    /api/reports/dashboard          → KPI summary
  GET    /api/reports/conversion         → Lead conversion rates
  GET    /api/reports/sales-by-rep       → Sales per rep (MANAGER/ADMIN)
  GET    /api/reports/monthly-trend      → Revenue by month (12 months)
  GET    /api/reports/activity-summary   → Interaction activity metrics
  GET    /api/reports/export/pdf         → Download PDF report

NOTIFICATIONS
  GET    /api/notifications/my           → My unread notifications
  PATCH  /api/notifications/{id}/read    → Mark as read

AUDIT
  GET    /api/audit-log                  → Query audit records (filtered)

DOCUMENTS
  GET    /api/documents/{id}/download    → Download a file

SYSTEM
  GET    /api/version                    → App version (for auto-update check)
  GET    /actuator/health                → Health check (DB, Redis status)
```

---

## Spring Boot Project Structure

```
crm-backend/
├── src/main/java/com/crm/
│   ├── CrmApplication.java
│   │
│   ├── config/
│   │   ├── SecurityConfig.java              ← JWT filter chain, CORS, CSRF, role-based rules
│   │   ├── JwtTokenProvider.java            ← Generate, validate, refresh JWT tokens
│   │   ├── JwtAuthenticationFilter.java     ← Intercept every request, extract and validate JWT
│   │   ├── RateLimitingFilter.java          ← IP-based and endpoint-specific rate limiting
│   │   ├── RedisConfig.java                 ← Redis connection and cache manager
│   │   ├── AsyncConfig.java                 ← Thread pools for @Async (email, audit)
│   │   ├── SwaggerConfig.java               ← OpenAPI documentation config
│   │   └── AuditAspect.java                 ← AOP aspect for automatic audit logging
│   │
│   ├── controller/
│   │   ├── AuthController.java              ← /api/auth/login, /register, /refresh, /logout
│   │   ├── CustomerController.java          ← /api/customers CRUD + import + documents
│   │   ├── LeadController.java              ← /api/leads CRUD + stage transitions
│   │   ├── TaskController.java              ← /api/tasks CRUD + /my + /complete
│   │   ├── InteractionController.java       ← /api/interactions log + timeline
│   │   ├── ReportController.java            ← /api/reports dashboard + PDF export
│   │   ├── NotificationController.java      ← /api/notifications/my + mark read
│   │   ├── AuditLogController.java          ← /api/audit-log query
│   │   └── DocumentController.java          ← /api/documents download
│   │
│   ├── service/
│   │   ├── AuthService.java                 ← Registration, login, token refresh, lockout
│   │   ├── CustomerService.java             ← Business logic + role-based filtering
│   │   ├── LeadService.java                 ← Stage transition validation + business rules
│   │   ├── TaskService.java                 ← Assignment, completion, overdue detection
│   │   ├── InteractionService.java          ← Immutable logging + 24-hour edit window
│   │   ├── ReportService.java               ← Aggregated queries + caching
│   │   ├── PdfExportService.java            ← PDF generation with watermarks
│   │   ├── EmailService.java                ← Async email sending with retry
│   │   ├── NotificationService.java         ← Create and query notifications
│   │   ├── AuditLogService.java             ← Async audit record creation
│   │   ├── FileStorageService.java          ← File upload, validation, storage
│   │   └── ExcelImportService.java          ← Parse and validate Excel/CSV imports
│   │
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── CustomerRepository.java
│   │   ├── LeadRepository.java
│   │   ├── TaskRepository.java
│   │   ├── InteractionRepository.java
│   │   ├── DocumentRepository.java
│   │   ├── NotificationRepository.java
│   │   ├── AuditLogRepository.java
│   │   ├── EmailLogRepository.java
│   │   └── RefreshTokenRepository.java
│   │
│   ├── model/entity/
│   │   ├── User.java
│   │   ├── Customer.java
│   │   ├── Lead.java
│   │   ├── Task.java
│   │   ├── Interaction.java
│   │   ├── Document.java
│   │   ├── Notification.java
│   │   ├── AuditLog.java
│   │   ├── EmailLog.java
│   │   └── RefreshToken.java
│   │
│   ├── model/enums/
│   │   ├── UserRole.java                    ← ADMIN, MANAGER, SALES_REP
│   │   ├── CustomerStatus.java              ← ACTIVE, INACTIVE, ARCHIVED
│   │   ├── LeadStage.java                   ← NEW, CONTACTED, QUALIFIED, PROPOSAL, WON, LOST
│   │   ├── TaskStatus.java                  ← PENDING, IN_PROGRESS, DONE, OVERDUE
│   │   ├── TaskPriority.java                ← LOW, MEDIUM, HIGH, URGENT
│   │   ├── InteractionType.java             ← CALL, EMAIL, MEETING, NOTE
│   │   └── NotificationType.java            ← TASK_ASSIGNED, TASK_OVERDUE, LEAD_STAGE_CHANGED
│   │
│   ├── dto/
│   │   ├── request/
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── CustomerCreateRequest.java
│   │   │   ├── LeadCreateRequest.java
│   │   │   ├── TaskCreateRequest.java
│   │   │   ├── InteractionCreateRequest.java
│   │   │   └── StageChangeRequest.java
│   │   │
│   │   └── response/
│   │       ├── ApiResponse.java             ← Generic wrapper: { success, message, data }
│   │       ├── AuthResponse.java            ← { accessToken, refreshToken, role, name }
│   │       ├── CustomerListDTO.java         ← Masked phone, no full details
│   │       ├── CustomerDetailDTO.java       ← Full profile for authorized users
│   │       ├── LeadDTO.java
│   │       ├── TaskDTO.java
│   │       ├── DashboardDTO.java
│   │       ├── ConversionReportDTO.java
│   │       └── ImportResultDTO.java         ← { created: 400, failed: 100, errors: [...] }
│   │
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java      ← @ControllerAdvice — catches all exceptions
│   │   ├── ResourceNotFoundException.java
│   │   ├── BadRequestException.java
│   │   ├── UnauthorizedException.java
│   │   ├── AccessDeniedException.java
│   │   ├── ConflictException.java           ← 409 for duplicate email, optimistic lock failure
│   │   └── RateLimitExceededException.java
│   │
│   ├── scheduler/
│   │   ├── OverdueTaskScheduler.java        ← Runs every hour — marks overdue tasks
│   │   ├── EmailReminderScheduler.java      ← Runs at 8 AM — tasks due today
│   │   └── OverdueAlertScheduler.java       ← Runs at 9 AM — emails for overdue tasks
│   │
│   └── util/
│       ├── InputSanitizer.java              ← Strip HTML/XSS, validate formats
│       ├── FileValidator.java               ← Magic byte validation, allowed types, size check
│       └── CsvSanitizer.java                ← Strip formulas (=CMD()) from imported cells
│
├── src/main/resources/
│   ├── application.yml                      ← Dev config (references env vars for secrets)
│   ├── application-prod.yml                 ← Production config (HTTPS, WARN logging)
│   └── db/migration/
│       ├── V1__create_users_table.sql
│       ├── V2__create_customers_table.sql
│       ├── V3__create_leads_table.sql
│       ├── V4__create_tasks_table.sql
│       ├── V5__create_interactions_table.sql
│       ├── V6__create_documents_table.sql
│       ├── V7__create_notifications_table.sql
│       ├── V8__create_audit_log_table.sql
│       ├── V9__create_email_log_table.sql
│       └── V10__create_refresh_tokens_table.sql
│
├── src/test/java/com/crm/
│   ├── service/          ← Unit tests for all services
│   ├── controller/       ← Integration tests for all endpoints
│   ├── security/         ← SQL injection, XSS, JWT tamper, IDOR, rate limit tests
│   └── scheduler/        ← Overdue detection tests
│
├── Dockerfile
├── docker-compose.yml
├── .env.example                             ← Template for environment variables
├── .gitignore                               ← Includes .env, uploaded files, IDE configs
├── README.md
└── pom.xml
```

---

## JavaFX Desktop App Structure

```
crm-desktop/
├── src/main/java/com/crm/desktop/
│   ├── CrmDesktopApp.java                  ← Main entry point, launches JavaFX
│   │
│   ├── api/
│   │   ├── ApiClient.java                  ← HTTP client wrapper, adds JWT to every request
│   │   ├── AuthApi.java                    ← Login, register, refresh token calls
│   │   ├── CustomerApi.java               ← Customer CRUD API calls
│   │   ├── LeadApi.java
│   │   ├── TaskApi.java
│   │   ├── InteractionApi.java
│   │   ├── ReportApi.java
│   │   ├── NotificationApi.java
│   │   └── DocumentApi.java
│   │
│   ├── controller/                         ← JavaFX screen controllers (FXML)
│   │   ├── LoginController.java
│   │   ├── MainController.java            ← Sidebar navigation, notification bell
│   │   ├── CustomerListController.java
│   │   ├── CustomerDetailController.java
│   │   ├── CustomerFormController.java
│   │   ├── LeadPipelineController.java
│   │   ├── LeadDetailController.java
│   │   ├── TaskListController.java
│   │   ├── TaskFormController.java
│   │   ├── InteractionTimelineController.java
│   │   ├── DashboardController.java
│   │   ├── ReportController.java
│   │   └── ImportController.java
│   │
│   ├── model/                              ← Client-side data models
│   │   ├── Customer.java
│   │   ├── Lead.java
│   │   ├── Task.java
│   │   ├── Interaction.java
│   │   └── Notification.java
│   │
│   ├── service/
│   │   ├── SessionManager.java            ← Stores JWT in memory, tracks inactivity timeout
│   │   ├── NotificationPoller.java        ← Background thread, polls every 30 seconds
│   │   └── DraftSaver.java               ← Auto-saves form drafts locally every 30 seconds
│   │
│   └── util/
│       ├── Validator.java                 ← Client-side input validation
│       ├── Formatter.java                 ← Number formatting, date formatting
│       └── AlertHelper.java              ← Reusable confirmation/error/success dialogs
│
├── src/main/resources/
│   ├── fxml/                              ← JavaFX FXML layout files
│   │   ├── login.fxml
│   │   ├── main.fxml
│   │   ├── customer-list.fxml
│   │   ├── customer-detail.fxml
│   │   ├── customer-form.fxml
│   │   ├── lead-pipeline.fxml
│   │   ├── task-list.fxml
│   │   ├── dashboard.fxml
│   │   └── report.fxml
│   │
│   ├── css/
│   │   └── styles.css                     ← Application theme
│   │
│   └── images/
│       ├── app-icon.png
│       └── splash.png
│
├── pom.xml
└── build-installer.bat                    ← Script to run jpackage and create .exe
```

---

# DETAILED BUILD PHASES

> **Every phase includes:**
> 1. **Goal** — what this phase achieves
> 2. **What to Build** — backend and desktop app tasks
> 3. **Security** — how to protect against attacks and unauthorized access
> 4. **Confidentiality** — how to protect sensitive data from exposure
> 5. **Reliability** — how to handle errors, prevent data loss, ensure correctness
> 6. **Loopholes & How to Prevent Them** — specific vulnerabilities and their fixes
> 7. **Tests to Write** — what to verify before moving to the next phase
> 8. **Deliverable** — what is complete at the end
> 9. **Prompt** — the exact prompt to use when building this phase with AI assistance

---

## PHASE 1 — Project Setup, Authentication & Security Foundation

**Weeks:** 1–2

**Goal:** Initialize both projects (Spring Boot backend + JavaFX desktop app), set up the database, and build a completely secure authentication system with login, registration, JWT tokens, role-based access, account lockout, and rate limiting.

---

### What to Build

#### Backend (Spring Boot)

**Project Initialization:**
- Create a Spring Boot 3 Maven project with dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `postgresql`, `flyway-core`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson`, `lombok`, `springdoc-openapi-starter-webmvc-ui`
- Configure `application.yml` with PostgreSQL connection using environment variable references (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`)
- Set up Flyway migration: `V1__create_users_table.sql` and `V10__create_refresh_tokens_table.sql`

**User Entity:**
- Fields: id (UUID, auto-generated), name (VARCHAR 100), email (VARCHAR 150, unique), password (VARCHAR 255, BCrypt hashed), role (enum: ADMIN/MANAGER/SALES_REP), enabled (boolean), failedLoginAttempts (int), accountLockedUntil (timestamp), createdAt, updatedAt
- Use `@PrePersist` and `@PreUpdate` for automatic timestamps

**Authentication Flow:**
- `POST /api/auth/register` — validate input → check email not taken → hash password with BCrypt (strength 12) → save user → return success message
- `POST /api/auth/login` — validate credentials → check account not locked → generate short-lived access token (15 min) → generate refresh token (7 days, stored in DB) → return both tokens + user role
- `POST /api/auth/refresh` — validate refresh token from DB → check not expired/revoked → issue new access token
- `POST /api/auth/logout` — revoke refresh token in DB → client clears tokens from memory

**JWT Implementation:**
- Access token: contains userId, email, role. Expiry: 15 minutes. Signed with HS256 using 256-bit secret from environment variable
- Refresh token: random UUID stored in `refresh_tokens` table. Expiry: 7 days. One active token per user (old ones revoked on new login)
- `JwtAuthenticationFilter` — intercepts every request, extracts token from `Authorization: Bearer <token>` header, validates signature and expiry, sets SecurityContext

**Security Configuration:**
- `SecurityConfig.java` — configure filter chain:
  - Permit: `/api/auth/login`, `/api/auth/register`, `/actuator/health`
  - Require authentication: everything else
  - Add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
  - Enable CORS with specific allowed origins (not wildcard `*`)
  - Enable CSRF protection
- `@PreAuthorize("hasRole('ADMIN')")`, `@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")` on controller methods

**Rate Limiting & Account Lockout:**
- `RateLimitingFilter.java` — track login attempts per IP using an in-memory map (or Redis). Max 5 attempts per minute per IP. Return 429 Too Many Requests if exceeded.
- On failed login: increment `failedLoginAttempts` on User entity. After 5 failures: set `accountLockedUntil` = now + 30 minutes. Return 403 "Account locked, try again in 30 minutes."
- On successful login: reset `failedLoginAttempts` to 0

**Global Error Handling:**
- `GlobalExceptionHandler.java` with `@ControllerAdvice`:
  - `ResourceNotFoundException` → 404
  - `BadRequestException` → 400
  - `UnauthorizedException` → 401
  - `AccessDeniedException` → 403
  - `ConflictException` → 409
  - `RateLimitExceededException` → 429
  - `Exception` (catch-all) → 500 with generic message "An unexpected error occurred" — NEVER expose stack trace
- All responses use `ApiResponse` DTO: `{ success: boolean, message: string, data: object }`

**Logging:**
- Configure Logback with structured format: `[timestamp] [level] [class] message`
- Log every login attempt: `"LOGIN_SUCCESS: email=ali@company.com, ip=192.168.1.5"` or `"LOGIN_FAILED: email=ali@company.com, ip=192.168.1.5, reason=BAD_PASSWORD"`
- Log account lockouts: `"ACCOUNT_LOCKED: email=ali@company.com, lockedUntil=2026-04-01T10:30:00"`
- NEVER log passwords, tokens, or request bodies

**Health Check:**
- Spring Actuator `/actuator/health` — exposed publicly (safe, shows only UP/DOWN)
- All other actuator endpoints disabled in production

#### Desktop App (JavaFX)

**Project Initialization:**
- Create a JavaFX 21 Maven project with `javafx-controls`, `javafx-fxml`, `javafx-graphics` dependencies
- Add `com.google.code.gson:gson` for JSON parsing
- Add Java HttpClient for API communication

**Login Screen (`login.fxml`):**
- Email text field, Password field (masked with dots), Login button, Register link
- On Login click: send POST to `/api/auth/login` with email + password as JSON
- On success: store access token + refresh token + user role in `SessionManager` (in-memory ONLY, never write to disk)
- On failure: show error label "Invalid credentials" (generic — never reveal if email exists or password is wrong)
- On network error: show "Cannot connect to server. Please check your connection."
- Loading spinner while request is in progress (don't freeze UI)

**Main Window (`main.fxml`):**
- Sidebar navigation: Customers, Leads, Tasks, Dashboard, Reports
- Top bar: logged-in user name + role badge + Notification bell + Logout button
- Content area: loads the selected screen

**Session Management:**
- `SessionManager.java`:
  - Stores JWT tokens in a private static field (memory only)
  - Adds `Authorization: Bearer <token>` header to every API request via `ApiClient`
  - Tracks last activity time. If idle for 15 minutes → auto-logout, clear all data, show login screen
  - On logout: set tokens to null, clear all cached data, navigate to login screen

---

### Security

| Threat | Protection |
|---|---|
| Plain-text password storage | BCrypt (strength 12) — passwords are one-way hashed |
| Brute-force login attacks | Rate limiting: 5 attempts/min/IP + Account lockout after 5 failures |
| JWT token theft | Short-lived access tokens (15 min) + refresh tokens stored in DB (revocable) |
| JWT tampering | HS256 signature validation — modified tokens are rejected |
| Secrets in source code | All secrets in environment variables, `.env` in `.gitignore` |
| Token persisted on disk | JWT stored only in memory — lost on app close |
| Session hijacking | Auto-logout after 15 minutes of inactivity |
| Stack trace leakage | GlobalExceptionHandler catches all errors, returns generic messages |
| Role escalation | `@PreAuthorize` on every endpoint — server-side role checks |

### Confidentiality

| Data | Protection |
|---|---|
| Passwords | BCrypt hashed — even admins cannot see plain-text passwords |
| JWT secret key | 256-bit key in environment variable — never in code or config files |
| DB credentials | In environment variables — `.env.example` has placeholders only |
| API responses | DTOs exclude password field — `@JsonIgnore` on password |
| Login error messages | Generic "Invalid credentials" — do not reveal if email exists |
| Logging | Log login events but NEVER log passwords, tokens, or request bodies |

### Reliability

| Failure Scenario | How We Handle It |
|---|---|
| Database down | Health check returns DOWN; login shows "Service unavailable" |
| Invalid JWT token | Return 401 Unauthorized — client redirects to login screen |
| Expired access token | Client uses refresh token to get a new access token silently |
| Expired refresh token | Force logout — "Your session has expired. Please log in again." |
| Network lost (desktop app) | Show banner "You are offline — cannot connect to server" |
| Malformed request | Bean Validation (`@Valid`) rejects immediately with 400 + field-level error messages |
| Unexpected server error | GlobalExceptionHandler returns 500 + "An unexpected error occurred" — no stack trace |

### Loopholes & Prevention

| Loophole | Risk | Prevention |
|---|---|---|
| No rate limiting on login | Attacker tries millions of passwords | Max 5 attempts/min/IP + account lockout |
| JWT never expires | Stolen token grants permanent access | Access token expires in 15 minutes |
| Refresh token not revocable | Compromised device keeps access | Refresh tokens stored in DB — revoked on logout |
| Weak JWT secret | Attacker can forge valid tokens | Minimum 256-bit secret, loaded from secure environment |
| Password returned in API response | Attacker reads hashed password from API | DTOs NEVER include password field |
| Same error for locked vs invalid | Attacker knows when account is locked | Use generic "Invalid credentials" for everything |
| Token stored in file on disk | Malware reads token from app data folder | Token stored ONLY in Java memory — never serialized to disk |

### Tests to Write

```
✓ Register with valid data → 201 Created, password is hashed in DB
✓ Register with duplicate email → 409 Conflict
✓ Register with invalid email format → 400 Bad Request
✓ Login with correct credentials → 200, returns access + refresh tokens
✓ Login with wrong password → 401, generic error message
✓ Login with non-existent email → 401, same generic error message
✓ Login 6 times with wrong password → Account locked for 30 minutes
✓ Login with locked account → 403 "Account locked"
✓ Access protected endpoint without token → 401 Unauthorized
✓ Access protected endpoint with expired token → 401
✓ Access protected endpoint with tampered token → 401
✓ Refresh token with valid refresh token → 200, new access token
✓ Refresh token with expired refresh token → 401
✓ Refresh token after logout (revoked) → 401
✓ Rate limiting → 6th request in 1 minute returns 429
✓ Health check → 200 UP
✓ Access ADMIN endpoint as SALES_REP → 403 Forbidden
```

### Deliverable

A secure authentication system where:
- Users register and log in via the JavaFX desktop app
- Passwords are hashed with BCrypt (never stored in plain text)
- Short-lived JWT tokens (15 min) with refresh token rotation
- Accounts lock after 5 failed login attempts
- Rate limiting prevents brute-force attacks
- No secrets in source code — all in environment variables
- Desktop app auto-logs out after 15 minutes of inactivity
- All errors return clean JSON responses — no stack traces exposed

---

### PROMPT — Phase 1

```
I am building a CRM desktop application with Java 17, Spring Boot 3 (backend API), and JavaFX 21 (desktop GUI). This is Phase 1: Project Setup & Authentication.

BACKEND (Spring Boot 3):
Create a Maven project with these dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, spring-boot-starter-actuator, postgresql, flyway-core, jjwt-api (0.12.x), jjwt-impl, jjwt-jackson, lombok, springdoc-openapi-starter-webmvc-ui.

Database: PostgreSQL. Use Flyway for migrations.

Create the User entity with fields: id (UUID, auto-generated), name, email (unique), password (BCrypt hashed, strength 12), role (enum: ADMIN, MANAGER, SALES_REP), enabled (boolean, default true), failedLoginAttempts (int, default 0), accountLockedUntil (Timestamp, nullable), createdAt, updatedAt (auto-set via @PrePersist/@PreUpdate).

Create AuthController with these endpoints:
- POST /api/auth/register → validate input with @Valid, check email not taken (return 409 if duplicate), hash password with BCrypt, save user, return ApiResponse with success message.
- POST /api/auth/login → validate credentials, check account not locked (if locked return 403 with lock expiry time), on success: reset failedLoginAttempts, generate access JWT (15 min expiry, contains userId + email + role, signed HS256 with 256-bit secret from env var JWT_SECRET), generate refresh token (random UUID, save to refresh_tokens table with 7-day expiry), return both tokens + user info. On failure: increment failedLoginAttempts, if >= 5 set accountLockedUntil = now + 30 min, return 401 generic "Invalid credentials".
- POST /api/auth/refresh → accept refresh token, validate it exists in DB and not expired/revoked, issue new access token, return it.
- POST /api/auth/logout → revoke the refresh token in DB.

Create JwtTokenProvider: generateAccessToken(user), generateRefreshToken(), validateToken(token), getUserIdFromToken(token). Secret key loaded from environment variable JWT_SECRET (minimum 256-bit).

Create JwtAuthenticationFilter (extends OncePerRequestFilter): extract token from Authorization Bearer header, validate it, set SecurityContext with the authenticated user.

Create SecurityConfig: permit /api/auth/login, /api/auth/register, /actuator/health. Require authentication for everything else. Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter. Configure CORS with specific origins (not wildcard). Enable CSRF.

Create RateLimitingFilter: use ConcurrentHashMap to track login attempts per IP. Max 5 per minute. Return 429 "Too many login attempts, please try again later" if exceeded. Clean up old entries every 5 minutes.

Create GlobalExceptionHandler with @ControllerAdvice: handle ResourceNotFoundException (404), BadRequestException (400), UnauthorizedException (401), AccessDeniedException (403), ConflictException (409), RateLimitExceededException (429), generic Exception (500 with message "An unexpected error occurred" — NEVER expose stack traces). All responses use ApiResponse DTO: { success: boolean, message: string, data: object }.

Configure Logback to log login attempts with timestamp and IP. NEVER log passwords, tokens, or request bodies.

Store all secrets (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET) in environment variables. Create .env.example with placeholder values. Add .env to .gitignore.

API responses must NEVER return the password field — use DTOs with @JsonIgnore on password.

DESKTOP APP (JavaFX 21):
Create a separate Maven project with javafx-controls, javafx-fxml, javafx-graphics, and Gson for JSON.

Build a Login Screen (login.fxml): email field, password field (masked), Login button. On click: send POST to backend /api/auth/login. On success: store access token and refresh token in SessionManager (in-memory ONLY — never write to file/disk). Navigate to Main Window. On failure: show error "Invalid credentials". On network error: show "Cannot connect to server." Show loading spinner during request.

Build Main Window (main.fxml): sidebar with navigation items (Customers, Leads, Tasks, Dashboard, Reports). Top bar with user name, role badge, notification bell placeholder, logout button. Content area for screen switching.

Build SessionManager: stores JWT in private static field (memory only). Adds Authorization Bearer header to all API calls via ApiClient. Tracks inactivity — auto-logout after 15 minutes idle. On logout: clear tokens, clear all cached data, show login screen.
```

---

## PHASE 2 — Customer Management with Data Protection

**Weeks:** 3–4

**Goal:** Build complete customer CRUD (Create, Read, Update, Delete) with strict role-based access control, PII masking, input validation, and protection against SQL injection and XSS.

---

### What to Build

#### Backend

**Customer Entity:**
- Fields: id (UUID), name (VARCHAR 150, not null), email (VARCHAR 150), phone (VARCHAR 20), company (VARCHAR 150), address (TEXT), city (VARCHAR 100), status (enum: ACTIVE/INACTIVE/ARCHIVED, default ACTIVE), assignedTo (FK to User), createdBy (FK to User), lastContactedAt (Timestamp), version (int, `@Version` for optimistic locking), createdAt, updatedAt

**Endpoints:**
- `GET /api/customers` — paginated (default 20, max 100), sortable (name, company, createdAt), filterable (name, company, status, city, assignedTo, dateRange). **CRITICAL**: SALES_REP sees only WHERE assigned_to = currentUserId. MANAGER sees WHERE assigned_to IN (team member IDs). ADMIN sees all.
- `POST /api/customers` — validate input with `@Valid`, sanitize all text (strip HTML tags), save, return created customer
- `GET /api/customers/{id}` — return full detail. Access check: SALES_REP can view only if assigned to them. MANAGER can view if assigned to their team. ADMIN can view all.
- `PUT /api/customers/{id}` — validate, sanitize, update. Same access rules. Uses `@Version` optimistic locking.
- `DELETE /api/customers/{id}` — ADMIN only. **Soft delete**: set status = ARCHIVED, never actually delete the row.

**DTOs:**
- `CustomerListDTO` — for list view: id, name, email, maskedPhone (e.g., `0300-***-4567`), company, status, assignedToName
- `CustomerDetailDTO` — for detail view: all fields including full phone, address, city, creation info

**Validation Rules (server-side — `@Valid` + Bean Validation):**
- name: `@NotBlank`, `@Size(max=150)`, custom validator to reject HTML tags
- email: `@Email` format validation
- phone: `@Pattern(regexp="...")` for valid phone format
- company: `@Size(max=150)`
- status: must be one of the allowed enum values
- assignedTo: must reference an existing, enabled user

**Input Sanitization:**
- `InputSanitizer.java` — strip all HTML tags using regex or OWASP Java HTML Sanitizer library
- Applied to: name, company, address, city — all free-text fields
- Prevents stored XSS attacks

#### Desktop App (JavaFX)

**Customer List Screen:**
- TableView: columns for Name, Email, Phone (masked), Company, Status, Assigned To
- Search bar: real-time search by name/company
- Filter dropdowns: Status, Assigned To (populated from API)
- Pagination: Previous/Next buttons, "Showing 1-20 of 342"
- "Add Customer" button (visible to all roles)
- "Delete" button (visible ONLY to ADMIN)

**Customer Form (Add/Edit):**
- Input fields: Name, Email, Phone, Company, Address, City, Status dropdown, Assigned To dropdown
- Client-side validation: red border on invalid fields, error message below each field
- Save button sends POST (create) or PUT (update) to API
- Cancel button closes the form

**Customer Detail Screen:**
- Full profile: all fields with unmasked phone number
- Tabs: Profile | Interactions (Phase 4) | Documents (Phase 7) | Activity Log (Phase 8)
- Edit button (disabled if user doesn't have access)
- Interaction timeline placeholder (built in Phase 4)

---

### Security

| Threat | Protection |
|---|---|
| Broken Access Control (OWASP #1) | Server-side row-level filtering: SALES_REP sees only their assigned customers |
| SQL Injection (OWASP #3) | JPA parameterized queries ONLY — never concatenate user input into SQL |
| XSS (Cross-Site Scripting) | InputSanitizer strips all HTML from text fields before saving |
| Mass Assignment | Use separate DTOs for input (CustomerCreateRequest) — only allowed fields are accepted |
| Forced Browsing | SALES_REP tries GET /api/customers/{otherId} → server checks ownership, returns 403 |
| Data Deletion by non-admin | DELETE endpoint checks role with @PreAuthorize("hasRole('ADMIN')") |
| Hard Delete (data loss) | Soft delete only — set status = ARCHIVED, rows are never removed |

### Confidentiality

| Data | Protection |
|---|---|
| Phone numbers (PII) | Masked in list view: `0300-***-4567`. Full in detail view only for authorized users. |
| Email addresses (PII) | Visible only to users who have access to that customer |
| Customer access | Logged: "User Ali viewed customer Ahmed's profile at 2:45 PM" |
| Data at rest | PostgreSQL TDE or encrypted disk volumes |

### Reliability

| Failure Scenario | How We Handle It |
|---|---|
| Two users edit same customer simultaneously | `@Version` optimistic locking → second save returns 409 Conflict |
| Assign customer to non-existent user | Validation rejects with 400 Bad Request |
| Duplicate email | Return 409 Conflict (not 500 Internal Server Error) |
| Network lost during save (desktop app) | Show "You are offline — changes cannot be saved" banner |
| Database transaction fails | `@Transactional` ensures complete rollback — no partial saves |

### Loopholes & Prevention

| Loophole | Risk | Prevention |
|---|---|---|
| No row-level access check | SALES_REP sees all 10,000 customers | WHERE clause filters by assigned_to on every query |
| IDOR: change customer ID in URL | Rep accesses another rep's customer | Server checks ownership before returning data |
| HTML in customer name | XSS when displayed | InputSanitizer strips all HTML before saving |
| Raw SQL in search filter | SQL injection | Use JPA Specifications — never build raw SQL strings |
| Hard delete removes evidence | Lost data, compliance violation | Soft delete only — ARCHIVED status |
| No optimistic locking | Two users overwrite each other's changes | @Version field, return 409 on conflict |

### Tests to Write

```
✓ Create customer with valid data → 201 Created
✓ Create customer with empty name → 400 Bad Request
✓ Create customer with invalid email → 400 Bad Request
✓ Create customer with HTML in name (<script>alert('xss')</script>) → saved with HTML stripped
✓ Create customer with duplicate email → 409 Conflict
✓ Get customer list as SALES_REP → only assigned customers returned
✓ Get customer list as MANAGER → only team's customers returned
✓ Get customer list as ADMIN → all customers returned
✓ Get customer detail as SALES_REP (not assigned) → 403 Forbidden
✓ Get customer detail as SALES_REP (assigned) → 200 OK with full data
✓ Update customer → 200, fields updated correctly
✓ Update customer with stale version → 409 Conflict (optimistic locking)
✓ Delete customer as SALES_REP → 403 Forbidden
✓ Delete customer as ADMIN → 200, status set to ARCHIVED (not deleted from DB)
✓ Search by name → returns matching results
✓ Pagination → correct page size and total count
```

### Deliverable

Complete customer management: add, view, edit, search, filter, soft-delete. Access strictly controlled by role. Phone numbers masked in list view. No SQL injection. No XSS. Optimistic locking prevents overwrite conflicts.

---

### PROMPT — Phase 2

```
I am building Phase 2 of my CRM desktop application (Java 17, Spring Boot 3 backend, JavaFX 21 desktop GUI). Phase 1 (authentication with JWT, rate limiting, account lockout) is already complete.

BACKEND (Spring Boot 3):
Create Customer entity: id (UUID), name (VARCHAR 150, @NotBlank), email (VARCHAR 150, @Email), phone (VARCHAR 20, @Pattern for valid format), company (VARCHAR 150), address (TEXT), city (VARCHAR 100), status (enum: ACTIVE/INACTIVE/ARCHIVED, default ACTIVE), assignedTo (ManyToOne → User), createdBy (ManyToOne → User), lastContactedAt (Timestamp), version (int, @Version for optimistic locking), createdAt, updatedAt.

Create Flyway migration V2__create_customers_table.sql.

Create CustomerRepository extending JpaRepository + JpaSpecificationExecutor (for dynamic filters).

Create CustomerService with CRITICAL role-based data filtering:
- SALES_REP: add WHERE assigned_to = currentUserId to every query (list, detail, update)
- MANAGER: add WHERE assigned_to IN (select id from users where manager_id = currentUserId)
- ADMIN: no filter, sees all
- Only ADMIN can delete (soft-delete: set status = ARCHIVED, never hard delete)
- On create/update: sanitize all text fields using InputSanitizer (strip HTML tags to prevent stored XSS)
- On create: check email not duplicate (return 409 Conflict if exists)
- On update: use @Version optimistic locking (return 409 Conflict on version mismatch)

Create InputSanitizer utility class: method sanitize(String input) that strips all HTML tags using regex or OWASP Java HTML Sanitizer. Apply to: name, company, address, city.

Create CustomerController:
- GET /api/customers → paginated (default 20, max 100), sortable by name/company/createdAt, filterable by name/company/status/city/assignedTo/dateRange. Use Spring Data Specifications for dynamic filtering. Return Page<CustomerListDTO> where phone is masked (0300-***-4567).
- POST /api/customers → @Valid input, sanitize, save, return CustomerDetailDTO.
- GET /api/customers/{id} → access check (SALES_REP can only view their assigned), return CustomerDetailDTO with full unmasked phone.
- PUT /api/customers/{id} → @Valid, access check, sanitize, update with optimistic locking, return CustomerDetailDTO.
- DELETE /api/customers/{id} → @PreAuthorize("hasRole('ADMIN')"), soft-delete only.

Create DTOs:
- CustomerCreateRequest: name, email, phone, company, address, city, assignedTo (UUID)
- CustomerListDTO: id, name, email, maskedPhone, company, status, assignedToName (phone masked as 0300-***-4567)
- CustomerDetailDTO: all fields fully visible including phone, address, city

All endpoints must validate input with @Valid and handle: duplicate email (409), customer not found (404), access denied (403), optimistic lock conflict (409).

DESKTOP APP (JavaFX):
Build Customer List Screen: TableView with columns (Name, Email, Phone masked, Company, Status, Assigned To). Search bar for real-time search. Filter dropdowns for Status and Assigned To. Pagination controls. "Add Customer" button for all users. "Delete" button visible ONLY for ADMIN role (HIDE it for others, don't just disable).

Build Customer Form (Add/Edit): fields with client-side validation — red border on invalid, error text below each field. Save and Cancel buttons. On save: POST or PUT to API. Show loading indicator.

Build Customer Detail Screen: full profile with unmasked phone. Edit button (disabled if not authorized). Placeholder tabs for Interactions, Documents, Activity Log (built in later phases).

Handle: 409 Conflict → "This record was modified by another user. Please refresh." Network error → "Cannot connect to server." Confirmation dialog before delete.
```

---

## PHASE 3 — Lead Pipeline & Task Management with Integrity Controls

**Weeks:** 5–6

**Goal:** Build the lead pipeline (Kanban-style stages with enforced transition rules) and task management (with priority, assignment, and auto-overdue detection). Enforce business rules that prevent data corruption.

---

### What to Build

#### Backend

**Lead Entity & Endpoints:**
- Entity: id (UUID), customerId (FK), title, stage (NEW→CONTACTED→QUALIFIED→PROPOSAL→WON/LOST), value (BigDecimal), expectedCloseDate, probability (0-100), lostReason (TEXT, only when LOST), ownerId (FK to User), version, createdAt, updatedAt
- CRUD + PATCH `/api/leads/{id}/stage` with **validated transitions**
- Stage transition rules in `LeadService`:
  - Can only move one step forward: NEW→CONTACTED→QUALIFIED→PROPOSAL→WON
  - Can move to LOST from any stage
  - Cannot skip stages (NEW→QUALIFIED = rejected)
  - Cannot reopen LOST without MANAGER/ADMIN approval
  - Cannot mark lead as WON if value = 0 or null
  - Cannot move backward without MANAGER approval
  - Must provide lostReason when moving to LOST

**Task Entity & Endpoints:**
- Entity: id (UUID), title, description, dueDate, priority (LOW/MEDIUM/HIGH/URGENT), status (PENDING/IN_PROGRESS/DONE/OVERDUE), customerId (FK), assignedTo (FK), createdBy (FK), version, createdAt, updatedAt
- CRUD + GET `/api/tasks/my` + PATCH `/api/tasks/{id}/complete`
- `OverdueTaskScheduler.java` — `@Scheduled(cron = "0 0 * * * *")` (every hour): find tasks WHERE dueDate < now AND status IN (PENDING, IN_PROGRESS) → set status = OVERDUE

**Access Control:**
- SALES_REP: sees/edits only leads where ownerId = currentUserId, tasks where assignedTo = currentUserId
- MANAGER: sees/edits team's leads and tasks, can reassign within team
- ADMIN: full access

#### Desktop App (JavaFX)

**Lead Pipeline Screen:** Kanban board with columns (NEW | CONTACTED | QUALIFIED | PROPOSAL | WON | LOST). Each lead is a card showing title + customer name + deal value. Button or drag to move between stages. Grey out invalid transitions.

**Task List Screen:** Table with filter by status (Pending/In Progress/Done/Overdue) and priority. Color-coded: overdue=red, urgent=orange, done=green. "My Tasks" view for sales reps.

---

### Security

| Threat | Protection |
|---|---|
| Unauthorized stage change | Server validates every transition against business rules + user role |
| Rep modifies another rep's lead | WHERE owner_id = currentUserId on every query |
| Bypassing stage rules via direct API call | Stage transition validation in LeadService — not just in UI |
| Setting lead value to negative | @DecimalMin("0.00") validation on value field |

### Confidentiality

| Data | Protection |
|---|---|
| Deal values | SALES_REP sees only their own. MANAGER sees team totals. ADMIN sees all. |
| Lost reasons | May contain competitor info — restricted to MANAGER+ |
| Stage change history | Logged for audit: "Lead moved from NEW to CONTACTED by Ali" |

### Reliability

| Failure Scenario | How We Handle It |
|---|---|
| Two reps move same lead simultaneously | @Version optimistic locking → 409 Conflict |
| Overdue scheduler fails | Logs error, retries next hour — tasks stay PENDING until caught |
| Invalid stage transition attempted | 400 Bad Request with clear message: "Cannot skip stages" |
| dueDate in the past on create | Validation rejects: "Due date must be today or in the future" |
| BigDecimal precision for deal values | Use BigDecimal (never double/float) — prevents rounding errors |

### Loopholes & Prevention

| Loophole | Risk | Prevention |
|---|---|---|
| No stage transition validation | Rep marks lead as WON immediately | Enforce sequential stages server-side |
| 0-value deal marked WON | False revenue reporting | Reject WON if value is 0 or null |
| No lostReason required | Losing leads without explanation | Require lostReason when stage = LOST |
| Overdue tasks not detected | Tasks silently expire | @Scheduled runs hourly to auto-mark OVERDUE |
| Double/float for money | Rounding errors in revenue | BigDecimal with scale(12,2) everywhere |

### Tests to Write

```
✓ Create lead → 201 Created
✓ Move lead NEW → CONTACTED → 200 OK, stage updated
✓ Move lead NEW → QUALIFIED (skip stage) → 400 "Cannot skip stages"
✓ Move lead to LOST without reason → 400 "Lost reason is required"
✓ Move lead to WON with value 0 → 400 "Deal value must be greater than zero"
✓ Move lead backward without MANAGER role → 403 Forbidden
✓ SALES_REP access another rep's lead → 403 Forbidden
✓ Create task with past due date → 400 "Due date must be today or later"
✓ Mark task complete → status = DONE
✓ Overdue scheduler: task with past dueDate → status = OVERDUE
✓ Concurrent lead update → 409 Conflict (optimistic locking)
✓ Create lead with negative value → 400 Bad Request
```

### Deliverable

Lead pipeline with Kanban UI and enforced stage transitions. Task management with priority and auto-overdue detection. Business rules prevent data corruption. All money uses BigDecimal.

---

### PROMPT — Phase 3

```
I am building Phase 3 of my CRM desktop application (Java 17, Spring Boot 3, JavaFX 21). Phase 1 (auth) and Phase 2 (customers) are complete.

BACKEND:
Create Lead entity: id (UUID), customerId (FK to Customer, @NotNull), title (@NotBlank, max 200), stage (enum: NEW/CONTACTED/QUALIFIED/PROPOSAL/WON/LOST, default NEW), value (BigDecimal, @DecimalMin("0.00")), expectedCloseDate (must be future on create), probability (int 0-100), lostReason (TEXT, required only when stage=LOST), ownerId (FK to User), version (@Version), createdAt, updatedAt.

Create Task entity: id (UUID), title (@NotBlank, max 200), description (TEXT), dueDate (Timestamp, @NotNull, must be today or future on create), priority (enum: LOW/MEDIUM/HIGH/URGENT, default MEDIUM), status (enum: PENDING/IN_PROGRESS/DONE/OVERDUE, default PENDING), customerId (FK to Customer), assignedTo (FK to User, @NotNull), createdBy (FK to User), version (@Version), createdAt, updatedAt.

Create Flyway migrations V3 and V4.

Create LeadService with STRICT stage transition validation:
- Allowed forward transitions: NEW→CONTACTED, CONTACTED→QUALIFIED, QUALIFIED→PROPOSAL, PROPOSAL→WON
- LOST allowed from any stage but requires lostReason
- Backward moves require MANAGER or ADMIN role
- Reopening LOST requires MANAGER or ADMIN
- WON requires value > 0
- Skipping stages is REJECTED with 400 "Cannot skip stages. Must go through [required stage]."
- Role-based filtering: SALES_REP sees WHERE owner_id = currentUserId. MANAGER sees team. ADMIN sees all.

Create TaskService:
- SALES_REP sees WHERE assigned_to = currentUserId
- MANAGER can reassign within team, ADMIN can reassign to anyone
- Cannot assign to disabled user
- Validate dueDate >= today on create
- GET /api/tasks/my returns tasks for logged-in user

Create OverdueTaskScheduler: @Scheduled(cron = "0 0 * * * *") — every hour: find tasks WHERE due_date < NOW() AND status IN ('PENDING','IN_PROGRESS'), set status = OVERDUE. Log count: "Marked X tasks as overdue."

Create LeadController: CRUD + PATCH /api/leads/{id}/stage (accepts StageChangeRequest with newStage and optional lostReason). Return 400 for invalid transitions with descriptive message.

Create TaskController: CRUD + GET /api/tasks/my + PATCH /api/tasks/{id}/complete.

All monetary values use BigDecimal. NEVER use double or float.

DESKTOP APP (JavaFX):
Build Lead Pipeline Screen: Kanban-style layout with 6 columns (NEW | CONTACTED | QUALIFIED | PROPOSAL | WON | LOST). Each lead card shows: title, customer name, deal value formatted with commas, expected close date. "Move to Next Stage" button on each card. Grey out transitions that are not allowed for the current user's role. Show warning dialog when moving backward. Show error if server rejects the transition.

Build Task List Screen: TableView with columns: Title, Customer, Due Date, Priority, Status, Assigned To. Filter by status dropdown and priority dropdown. Color-code rows: OVERDUE=red background, URGENT priority=orange text, DONE=green text. Default view: "My Tasks" for SALES_REP.

Build Add Task Form: title, description, due date picker (min=today), priority dropdown, customer dropdown, assigned-to dropdown. Validate locally before sending to server.
```

---

## PHASE 4 — Interaction Logging & Customer Timeline (Tamper-Proof Records)

**Weeks:** 7

**Goal:** Build an immutable interaction log — every call, email, and meeting with a customer is recorded and cannot be deleted. Edits are allowed only within 24 hours.

---

### What to Build

#### Backend

- `Interaction` entity: id (UUID), customerId (FK), type (CALL/EMAIL/MEETING/NOTE), subject (VARCHAR 200), notes (TEXT, max 5000 chars), duration (int, minutes), loggedBy (FK), createdAt (server-side only)
- `POST /api/interactions` — create, set createdAt server-side, sanitize notes (strip HTML), update customer's lastContactedAt atomically
- `GET /api/interactions/customer/{id}` — paginated, newest first. Access restricted to: author, assigned rep, team manager, admin
- `PUT /api/interactions/{id}` — ONLY within 24 hours of creation, ONLY by original author or ADMIN
- **NO DELETE ENDPOINT** — interactions are permanent, append-only records
- Rate limit: max 100 per user per hour

#### Desktop App (JavaFX)

- Interaction Tab inside Customer Detail: scrollable timeline with icons (phone/email/meeting/note)
- "Log Interaction" form: type dropdown, subject, notes (with character counter), duration (optional)
- Edit button disabled after 24 hours with tooltip explanation
- Filter by type

---

### Security

| Threat | Protection |
|---|---|
| Deleting evidence | No delete endpoint exists — append-only |
| Editing old records to change history | 24-hour edit window, then records are frozen |
| Timestamp manipulation | createdAt set server-side — client input ignored |
| XSS in notes | InputSanitizer strips HTML from notes before saving |
| Spam/abuse | Rate limit: 100 interactions per user per hour |

### Confidentiality

| Data | Protection |
|---|---|
| Conversation notes | Access restricted to: author + assigned rep + manager + admin |
| Notes not in search index | Searchable by customer, NOT by note content |

### Reliability

| Scenario | Handling |
|---|---|
| App crashes while typing notes | Auto-save draft locally every 30 seconds |
| Customer doesn't exist | Return 404, not 500 |
| Two users log interaction simultaneously | No conflict — interactions are append-only |
| Interaction + lastContactedAt update | @Transactional — both succeed or both fail |

### Loopholes & Prevention

| Loophole | Risk | Prevention |
|---|---|---|
| No edit time limit | Records altered weeks later | 24-hour window enforced server-side |
| Client sends custom createdAt | Fake timestamps | createdAt set by server — client value ignored |
| Delete via direct API | Evidence destroyed | No DELETE endpoint exists in controller |
| Notes with HTML/scripts | XSS attack | Sanitized before saving |

### Tests to Write

```
✓ Create interaction → 201, createdAt is server time (not client)
✓ Edit within 24 hours by author → 200 OK
✓ Edit after 24 hours → 403 "Cannot edit — 24 hour window expired"
✓ Edit by non-author (not admin) → 403 Forbidden
✓ DELETE request → 405 Method Not Allowed (no endpoint)
✓ Notes with <script> tags → saved with HTML stripped
✓ Get interactions for customer (authorized) → 200 with timeline
✓ Get interactions for customer (unauthorized) → 403
✓ Rate limit: 101st interaction in 1 hour → 429
✓ Create interaction for non-existent customer → 404
```

### Deliverable

Immutable interaction history. 24-hour edit window. Server-side timestamps. No deletions possible. Draft auto-save in desktop app.

---

### PROMPT — Phase 4

```
I am building Phase 4 of my CRM desktop application (Java 17, Spring Boot 3, JavaFX 21). Phases 1-3 are complete.

BACKEND:
Create Interaction entity: id (UUID), customerId (FK to Customer, @NotNull), type (enum: CALL/EMAIL/MEETING/NOTE, @NotNull), subject (VARCHAR 200), notes (TEXT, max 5000 chars), duration (int, nullable, for calls/meetings), loggedBy (FK to User, set automatically from JWT), createdAt (set server-side via @PrePersist — IGNORE any client-provided value).

Flyway migration V5__create_interactions_table.sql.

Create InteractionService:
- Create: validate customer exists, sanitize subject and notes (strip HTML), set createdAt = now() on server, set loggedBy = current user from JWT, update customer.lastContactedAt atomically within @Transactional.
- Edit: allowed ONLY if (createdAt + 24 hours > now()) AND (loggedBy = currentUser OR currentUser.role = ADMIN). Otherwise return 403 "Cannot edit — edit window expired."
- NO DELETE METHOD. Do not create a delete endpoint. Interactions are immutable append-only records.
- List by customer: paginated, sorted newest first. Access check: user must be the author, OR assigned to the customer, OR MANAGER of the team, OR ADMIN.
- Rate limit: max 100 interactions per user per hour.

Create InteractionController:
- POST /api/interactions → create
- GET /api/interactions/customer/{customerId} → list for customer (paginated)
- GET /api/interactions/recent → last 50 across all accessible customers
- PUT /api/interactions/{id} → edit (within 24h only)
- NO DELETE endpoint

DESKTOP APP (JavaFX):
Add Interaction Tab to Customer Detail Screen. Show scrollable timeline: each entry has icon (phone/email/meeting/note), subject, date formatted nicely, logged-by name. Click to expand and see full notes.

"Log Interaction" button opens form: type dropdown, subject text field, notes textarea (with live character counter showing "1234 / 5000"), duration spinner (optional, for calls/meetings). Save and Cancel buttons.

Edit button on each interaction — disabled with tooltip "Cannot edit — logged more than 24 hours ago" if older than 24h. Active only for the original author or admin within 24h.

Filter bar above timeline: filter by type (All, Calls, Emails, Meetings, Notes).

Auto-save draft: save form content to a local temp file every 30 seconds. On app restart, if draft exists, prompt "You have an unsaved draft. Restore it?"
```

---

## PHASE 5 — Dashboard & Reports with Data Accuracy Guarantees

**Weeks:** 8–9

**Goal:** Build the analytics dashboard with KPI cards and JavaFX charts. All numbers must be mathematically precise (BigDecimal), cached (Redis), and role-filtered.

---

### What to Build

#### Backend

- `GET /api/reports/dashboard` → totalCustomers, openLeads, tasksDueToday, tasksOverdue, totalRevenue (sum of WON lead values), last updated timestamp
- `GET /api/reports/conversion` → leads by stage (count per stage), win rate, average deal size, average days to close
- `GET /api/reports/sales-by-rep` → per rep: name, dealsWon, dealValue, winRate (MANAGER/ADMIN only)
- `GET /api/reports/monthly-trend` → revenue per month for last 12 months
- `GET /api/reports/activity-summary` → interactions logged per day/week/month
- All queries role-filtered. Redis cache with 5-min TTL. Cache keys include userId. BigDecimal for all money. Handle division by zero.

#### Desktop App (JavaFX)

- Dashboard: KPI cards + BarChart (sales by rep) + PieChart (leads by stage) + LineChart (monthly revenue)
- Refresh button + "Last updated" timestamp
- Numbers formatted with commas and currency symbols

---

### Security, Confidentiality, Reliability, Loopholes

Same structure as previous phases — role filtering on all queries, BigDecimal precision, Redis caching with user-scoped keys, rate limiting on report endpoints (10/min), division-by-zero protection, loading states.

### Tests to Write

```
✓ Dashboard as SALES_REP → only own numbers
✓ Dashboard as ADMIN → all numbers
✓ Conversion with 0 leads → 0% (not NaN)
✓ Revenue uses BigDecimal (verify no rounding errors)
✓ Cache hit → returns cached data
✓ Cache invalidation after lead stage change → fresh data
✓ Sales-by-rep as SALES_REP → 403 Forbidden
```

### Deliverable

Accurate, cached, role-filtered dashboard with live charts. BigDecimal precision. No division-by-zero crashes.

---

### PROMPT — Phase 5

```
I am building Phase 5 of my CRM desktop application (Java 17, Spring Boot 3, JavaFX 21). Phases 1-4 are complete.

BACKEND:
Create ReportService with these methods (all role-filtered: SALES_REP sees own data, MANAGER sees team, ADMIN sees all):

1. getDashboard(userId, role): returns DashboardDTO with totalCustomers (long), openLeads (long), tasksDueToday (long), tasksOverdue (long), totalRevenue (BigDecimal — sum of value from leads WHERE stage=WON), lastUpdatedAt (LocalDateTime).

2. getConversionReport(userId, role): returns ConversionReportDTO with leadsPerStage (Map<LeadStage, Long>), winRate (BigDecimal — won / total leads * 100, handle 0 leads = 0% not NaN), averageDealSize (BigDecimal), averageDaysToClose (BigDecimal).

3. getSalesByRep(userId, role): restricted to MANAGER and ADMIN. Returns List<SalesRepDTO> with repName, dealsWon (long), totalValue (BigDecimal), winRate (BigDecimal).

4. getMonthlyTrend(userId, role): returns List<MonthlyTrendDTO> with month (String "2026-01"), revenue (BigDecimal), leadCount (long) for last 12 months.

5. getActivitySummary(userId, role): returns interactions logged counts per day/week/month.

CRITICAL: All monetary calculations must use BigDecimal. NEVER use double or float. Handle division by zero: if denominator is 0, return BigDecimal.ZERO.

Caching: Use Redis. Cache dashboard data with key "dashboard:{userId}" and TTL 5 minutes. Invalidate cache when leads change stage or tasks are completed (use @CacheEvict or manual cache invalidation in LeadService/TaskService).

Rate limit: max 10 requests per minute per user on all report endpoints.

Create ReportController:
- GET /api/reports/dashboard
- GET /api/reports/conversion
- GET /api/reports/sales-by-rep → @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
- GET /api/reports/monthly-trend
- GET /api/reports/activity-summary

DESKTOP APP (JavaFX):
Build Dashboard Screen:
- Top row: 5 KPI cards (Total Customers, Open Leads, Tasks Due Today, Overdue Tasks, Revenue formatted with commas: "Rs. 1,234,567").
- Middle row: JavaFX BarChart (sales by rep — rep names on X axis, deal value on Y), JavaFX PieChart (leads by stage — one slice per stage with percentage labels).
- Bottom row: JavaFX LineChart (monthly revenue trend — months on X, revenue on Y).
- "Last updated: 2 minutes ago" label. Refresh button to reload all data. Auto-refresh every 5 minutes.
- Loading skeletons while data loads. If any report fails, show "Unable to load" with Retry button — don't crash the whole dashboard.
- Format all numbers: commas for thousands, 2 decimal places for percentages.
```

---

## PHASE 6 — PDF Reports & Email Notifications with Secure Delivery

**Weeks:** 10

**Goal:** Generate PDF reports with watermarks and send automated email reminders securely over TLS.

---

### What to Build

- PDF: iText/JasperReports. Monthly sales report + individual customer summary. Watermark: "Generated by [User] — CONFIDENTIAL". Streamed, never stored on disk. MANAGER/ADMIN only.
- Email: JavaMailSender + TLS. Welcome email on customer creation. Scheduled reminders at 8-9 AM. Async with 3x retry. EmailLog table for tracking. No sensitive data in email body.

### PROMPT — Phase 6

```
I am building Phase 6 of my CRM desktop application (Java 17, Spring Boot 3, JavaFX 21). Phases 1-5 are complete.

BACKEND:
PDF Generation:
- Add iText 7 dependency to pom.xml
- Create PdfExportService:
  - generateMonthlySalesReport(userId, role): creates a PDF with title "Monthly Sales Report — [Month Year]", company header, table of leads won (title, customer, value, close date), total revenue at bottom, bar chart of sales by rep. Add watermark on every page: "Generated by [UserName] on [DateTime] — CONFIDENTIAL — Internal Use Only". Return byte[] (stream directly to client — NEVER save to disk).
  - generateCustomerSummaryReport(customerId, userId, role): creates PDF with customer profile, interaction timeline, lead history. Same watermark.
- Restrict both to @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
- GET /api/reports/export/pdf → streams PDF with Content-Type application/pdf
- GET /api/reports/export/pdf?type=customer&id={customerId} → customer-specific PDF

Email Notifications:
- Configure JavaMailSender in application.yml using environment variables: MAIL_HOST, MAIL_PORT (587), MAIL_USERNAME, MAIL_PASSWORD. Use STARTTLS for TLS encryption.
- Create EmailService (@Async — runs in separate thread pool configured in AsyncConfig):
  - sendEmail(to, subject, body): validate email address (no newlines/BCC injection), send via JavaMailSender, log result to email_log table (recipient, subject, status SENT/FAILED, error message if failed).
  - Retry logic: if sending fails, retry 3 times with exponential backoff (1 second, 5 seconds, 15 seconds). After 3 failures, log as FAILED and move on.
- Create email_log table via Flyway migration.
- Email content rules: NEVER include customer names, phone numbers, or deal values in email body. Use generic messages: "You have 3 overdue tasks. Please log in to the CRM app to view details."

Schedulers:
- EmailReminderScheduler: @Scheduled(cron = "0 0 8 * * *") — every day at 8 AM: find tasks WHERE due_date = today AND status = PENDING, send email to assigned_to: "You have tasks due today."
- OverdueAlertScheduler: @Scheduled(cron = "0 0 9 * * *") — every day at 9 AM: find tasks WHERE status = OVERDUE, group by assigned_to, send email: "You have X overdue tasks."
- On customer creation: send welcome email (async).

IMPORTANT: If SMTP server is unreachable, log the error and continue. Email failure must NEVER crash or block the application.

DESKTOP APP (JavaFX):
Add "Download PDF Report" button on Reports screen. On click: call API, receive byte[], open JavaFX FileChooser save dialog (default filename: "CRM-Report-2026-03.pdf"), save to chosen location. Show progress indicator during generation. Show success notification "Report saved to [path]" or error "Report generation failed. Please try again."
```

---

## PHASE 7 — File Upload & Excel Import with Malware Protection

**Weeks:** 11

**Goal:** Secure file attachment for customers and safe bulk import from Excel/CSV. Validate file content (not just extension) to block malicious uploads.

---

### What to Build

- Upload: POST multipart, validate by magic bytes (not extension), max 10MB, allowed: PDF/DOCX/XLSX/JPG/PNG. Randomize stored filenames (UUID). Path traversal prevention. Store outside web root.
- Import: POST .xlsx/.csv, max 5000 rows, batch process (100 rows/commit), sanitize cells (strip =CMD() formulas), detailed error report.
- Documents inherit customer's access rules.

### PROMPT — Phase 7

```
I am building Phase 7 of my CRM desktop application (Java 17, Spring Boot 3, JavaFX 21). Phases 1-6 are complete.

BACKEND:
File Upload:
- Create documents table (Flyway migration): id (UUID), customer_id (FK), original_filename (VARCHAR 255), stored_path (VARCHAR 500), file_size (BIGINT), mime_type (VARCHAR 100), deleted (BOOLEAN default false), uploaded_by (FK to User), uploaded_at (TIMESTAMP).
- Create FileValidator utility:
  - Allowed types: PDF, DOCX, XLSX, JPG, JPEG, PNG
  - Validate by reading file's first bytes (magic bytes/file signature) — NOT by file extension. This prevents renaming malware.exe to report.pdf.
  - Max file size: 10 MB (also configured in application.yml: spring.servlet.multipart.max-file-size=10MB)
  - Strip all path traversal characters from filename: remove ../, ..\, /, \
- Create FileStorageService:
  - save(MultipartFile, customerId): validate file → generate random stored name (UUID + extension, e.g. "a1b2c3d4.pdf") → save to configured upload directory (OUTSIDE web root, e.g. /var/crm/uploads/) → save metadata to documents table → return DocumentDTO
  - Files stored with restricted OS permissions (read/write only by app user)
  - On DB insert failure: delete the orphaned file (transactional consistency)
- POST /api/customers/{id}/documents → upload (authenticated, must have access to customer)
- GET /api/customers/{id}/documents → list (access check: same rules as customer access)
- GET /api/documents/{id}/download → stream file (access check)
- DELETE /api/documents/{id} → soft-delete (set deleted=true), keep file for 30 days, then purge

Excel Import:
- Create ExcelImportService using Apache POI:
  - POST /api/customers/import → accepts .xlsx or .csv
  - Max 5000 rows per upload (reject larger files immediately)
  - Create CsvSanitizer: strip formula injection — any cell starting with =, +, -, @, |, %0D, TAB is stripped to prevent CSV injection attacks
  - Validate each row: name required, email format, phone format
  - Batch processing: commit every 100 rows. If rows 101-200 fail, rows 1-100 are already saved.
  - Return ImportResultDTO: { created: 400, failed: 100, errors: [{ row: 15, field: "email", message: "Invalid format" }, ...] }
  - Handle corrupt files: catch POI exceptions → return 400 "Invalid file format"

DESKTOP APP (JavaFX):
Customer Detail Screen: "Attach Document" button → JavaFX FileChooser filtered to allowed types. Show upload progress bar. Document list table: filename, type, size (formatted: "2.4 MB"), uploaded by, date, Download and Delete buttons.

Customer List Screen: "Import Customers" button → FileChooser for .xlsx/.csv. Show import progress: "Importing... 340 of 500". Result popup: "Created: 400, Failed: 100" with "View Errors" button showing detailed error list.

Validate file type and size on client-side before uploading (instant rejection). Cancel button for long imports.
```

---

## PHASE 8 — Audit Trail, Notifications & Security Hardening

**Weeks:** 12–13

**Goal:** Complete audit trail for accountability. Real-time in-app notifications. Production-level security hardening with OWASP best practices.

---

### What to Build

- **Audit Log**: Auto-captured via Spring AOP on every create/update/delete. Immutable (no edit/delete endpoints). Fields: entityType, entityId, action, fieldName, oldValue, newValue, performedBy, performedAt, ipAddress. Never store password changes in audit.
- **Notifications**: In-app bell with unread count. System tray popups. Background polling every 30 seconds with JWT refresh.
- **Hardening**: HTTP security headers (HSTS, nosniff, DENY frame), OWASP dependency scan, API versioning (/api/v1/), encrypted database backups, request logging (no bodies), circuit breakers for external services.

### PROMPT — Phase 8

```
I am building Phase 8 of my CRM desktop application (Java 17, Spring Boot 3, JavaFX 21). Phases 1-7 are complete.

BACKEND:

Audit Trail:
- Create AuditLog entity: id (UUID), entityType (VARCHAR: CUSTOMER/LEAD/TASK), entityId (UUID), action (VARCHAR: CREATE/UPDATE/DELETE/STAGE_CHANGE), fieldName (VARCHAR 100, nullable), oldValue (TEXT, nullable), newValue (TEXT, nullable), performedBy (FK to User), performedAt (TIMESTAMP, server-side), ipAddress (VARCHAR 45).
- Flyway migration V8__create_audit_log_table.sql.
- Create AuditAspect using Spring AOP (@AfterReturning on service methods for create/update/delete): automatically capture changes and write to audit_log. Run @Async so audit logging never blocks the main operation.
- CRITICAL: NEVER log password field changes in audit. If field is "password", log action as "PASSWORD_CHANGED" with oldValue and newValue = null.
- AuditLog is IMMUTABLE: provide only GET /api/audit-log (query with filters: entityType, entityId, performedBy, dateRange). NO PUT, NO DELETE endpoints.
- GET /api/audit-log?entityType=customer&entityId={id} → full change history
- GET /api/audit-log?performedBy={userId}&from=2026-01-01&to=2026-03-31 → user activity report
- Restrict: MANAGER can view audit for their team. ADMIN sees all.

Notifications:
- Create Notification entity: id (UUID), recipientId (FK to User), title (VARCHAR 200), message (TEXT), type (enum: TASK_ASSIGNED/TASK_OVERDUE/LEAD_STAGE_CHANGED/CUSTOMER_ASSIGNED), read (boolean, default false), createdAt.
- Flyway migration V7__create_notifications_table.sql.
- Auto-create notifications: when task is assigned → notify assignee "You have a new task: [title]", when lead stage changes → notify owner, when customer is assigned → notify rep. Messages: NEVER include sensitive data — "You have a new task" not "Call Ahmed at 0300-1234567".
- GET /api/notifications/my → unread + last 50 for current user
- PATCH /api/notifications/{id}/read → mark as read

Security Hardening:
- Add HTTP security headers in SecurityConfig:
  - X-Content-Type-Options: nosniff
  - X-Frame-Options: DENY
  - Strict-Transport-Security: max-age=31536000; includeSubDomains (HSTS)
  - Cache-Control: no-store on all authenticated endpoints
  - Content-Security-Policy: default-src 'self'
- Implement API versioning: change all endpoints to /api/v1/... (e.g., /api/v1/customers)
- Add request logging filter: log every request (method, URL, userId, IP, responseCode, latencyMs). NEVER log request bodies.
- Add IP-based global rate limiting: max 200 requests/minute/IP
- Disable all Spring Actuator endpoints except /health and /info in production profile
- Add OWASP Dependency-Check Maven plugin: fail build if critical vulnerabilities are found
- Database backups: create a script for daily pg_dump, encrypt with AES-256, store separately
- Add Resilience4j circuit breaker on EmailService and FileStorageService (S3): if service is down, fail fast in 5 seconds instead of hanging for 30 seconds

DESKTOP APP (JavaFX):
Activity Log Tab on Customer Detail: show timeline "Ali changed status from ACTIVE to ARCHIVED on March 15 at 2:30 PM", "Sara created this customer on March 1 at 10:00 AM".

Notification Bell in top-right of Main Window: shows unread count badge (red circle with number). Click opens dropdown with recent notifications. Click a notification to navigate to the relevant screen (e.g., click "New task" → opens Task detail).

System Tray Icon: when app is minimized, show icon in Windows system tray. On new urgent notification, show OS-level popup notification.

NotificationPoller: background thread that calls GET /api/notifications/my every 30 seconds. Uses JWT. If access token expired, auto-refresh using refresh token. If refresh token expired, force logout: "Session expired. Please log in again."

Polish: consistent theme colors, loading spinners on every screen, proper empty states ("No customers yet — add your first one!"), error messages for every possible failure, keyboard shortcuts (Ctrl+N, Ctrl+F, Ctrl+R, Escape).
```

---

## PHASE 9 — Testing, Packaging & Secure Deployment

**Weeks:** 14–16

**Goal:** Thoroughly test everything (unit + integration + security), package as a Windows `.exe` installer, and deploy the backend via Docker with a complete security checklist.

---

### What to Build

**Testing:**
- Unit tests: all services, JWT, validation (80%+ coverage target)
- Integration tests: full API flows with Testcontainers (PostgreSQL)
- Security tests: SQL injection, XSS, IDOR, JWT tampering, path traversal, rate limiting
- Performance tests: dashboard with 10K customers, search with 50 concurrent users

**Packaging:**
- Backend: Docker multi-stage build + docker-compose (Spring Boot + PostgreSQL + Redis)
- Desktop: jpackage → `.exe` installer with bundled JRE, app icon, splash screen, auto-update check
- Code-signing certificate on `.exe`

**Deployment Checklist:**
- Secrets in env vars, HTTPS/SSL, DB backups tested, rate limiting active, Actuator locked down, OWASP scan clean, default accounts removed, CORS restricted, file directory permissions set, README complete

### PROMPT — Phase 9

```
I am building Phase 9 (final phase) of my CRM desktop application (Java 17, Spring Boot 3, JavaFX 21). Phases 1-8 are complete with all features built.

TESTING:

Unit Tests (JUnit 5 + Mockito):
- AuthService: register (valid, duplicate email), login (valid, wrong password, locked account), JWT generation/validation/expiry/tamper, refresh token, rate limiting
- CustomerService: CRUD, role-based filtering (SALES_REP only sees own), input sanitization (HTML stripped), duplicate email handling, optimistic locking
- LeadService: stage transitions (valid forward, invalid skip, backward needs MANAGER, WON needs value>0, LOST needs reason), role-based access
- TaskService: create (valid, past due date rejected), complete, overdue detection
- InteractionService: create, edit within 24h, edit after 24h (rejected), no delete
- ReportService: dashboard accuracy, conversion rate with 0 leads (no NaN), BigDecimal precision
- PdfExportService: generates valid PDF, includes watermark
- EmailService: send (mock SMTP), retry logic (3 attempts), email injection prevention
- FileValidator: valid types accepted, invalid types rejected, magic byte validation, oversized file rejected, path traversal stripped
- ExcelImportService: valid import, corrupt file handling, CSV injection stripped, row limit enforced

Integration Tests (SpringBootTest + Testcontainers with PostgreSQL):
- Full flow: register → login → create customer → create lead → move stages → create task → log interaction → view dashboard → export PDF
- Access control: login as SALES_REP → GET /api/customers/{otherRepsCustomer} → expect 403
- Concurrent update: two requests update same customer → one gets 409
- Rate limiting: 6 login attempts in 1 minute → 429

Security Tests:
- SQL injection: search customer with name = "'; DROP TABLE customers;--" → no effect, returns 0 results
- XSS: create customer with name = "<script>alert('xss')</script>" → saved with HTML stripped
- Path traversal: upload file with name "../../etc/passwd" → name sanitized, stored as UUID
- JWT tamper: modify token payload → 401 Unauthorized
- IDOR: SALES_REP accesses customer/{otherRepsId} → 403
- Email injection: send to "ali@mail.com\nBCC:hacker@evil.com" → rejected

Target: 80%+ code coverage on backend.

PACKAGING:

Backend Docker:
Create Dockerfile (multi-stage):
  Stage 1: FROM maven:3.9-eclipse-temurin-17 AS build → copy source → mvn clean package -DskipTests
  Stage 2: FROM eclipse-temurin:17-jre-alpine → copy jar from build stage → ENTRYPOINT java -jar app.jar

Create docker-compose.yml:
  services:
    app: build . → ports 8080 → env_file .env → depends_on db, redis
    db: postgres:16-alpine → volume for persistence → env POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD
    redis: redis:7-alpine → port 6379

Create .env.example with all required variables:
  DB_URL=jdbc:postgresql://db:5432/crm
  DB_USERNAME=crm_user
  DB_PASSWORD=change_me
  JWT_SECRET=change_me_256_bit_minimum
  MAIL_HOST=smtp.gmail.com
  MAIL_PORT=587
  MAIL_USERNAME=your@email.com
  MAIL_PASSWORD=app_password

Create application-prod.yml: secrets from env, connection pool max 20, logging WARN except security=INFO, HTTPS config placeholder.

Desktop App Packaging:
Create build-installer.bat script that runs:
  mvn clean package
  jpackage --input target/ --name "CRM Desktop" --main-jar crm-desktop.jar --main-class com.crm.desktop.CrmDesktopApp --type exe --icon src/main/resources/images/app-icon.ico --app-version 1.0.0 --vendor "CRM System" --win-dir-chooser --win-shortcut --java-options "-Xmx512m"

Add auto-update check: on startup, call GET /api/version → compare with local version → if newer: show banner "Version X.Y available. Please download the update."

README.md: project overview, features list, tech stack, prerequisites, setup steps (clone, create .env, docker-compose up), API documentation link (/swagger-ui.html), desktop app installation guide, screenshots.

DEPLOYMENT SECURITY CHECKLIST (verify all before going live):
□ All secrets in environment variables
□ HTTPS with valid SSL certificate
□ Database backups daily (encrypted, tested restore)
□ Rate limiting on login + reports + global IP limit
□ Spring Actuator: only /health and /info exposed
□ OWASP dependency scan: 0 critical vulnerabilities
□ JWT secret: 256-bit minimum
□ All test/default accounts removed
□ Logging: security events=INFO, everything else=WARN
□ CORS: specific origins only (no wildcard *)
□ File upload directory: restricted OS permissions
□ HTTP headers: HSTS, nosniff, DENY, no-store
□ No stack traces in API error responses
□ Code-signing certificate on .exe installer
□ README and Swagger docs complete
```

---

# SUMMARY TIMELINE

| Phase | What You Build | Key Security/Reliability Pillars | Weeks |
|---|---|---|---|
| **1** | Project setup + Secure Login/Register | BCrypt, JWT with refresh, account lockout, rate limiting, env vars | 1–2 |
| **2** | Customer CRUD with access control | Role-based row filtering, PII masking, XSS/SQLi prevention, optimistic locking | 3–4 |
| **3** | Lead pipeline + Task management | Stage transition rules, BigDecimal money, auto-overdue scheduler | 5–6 |
| **4** | Interaction logging + Timeline | Immutable records, 24h edit window, server-side timestamps, no delete | 7 |
| **5** | Dashboard + Charts + Reports | BigDecimal precision, Redis caching, role-filtered, division-by-zero safe | 8–9 |
| **6** | PDF export + Email reminders | PDF watermarks, TLS email, async retry, no PII in emails | 10 |
| **7** | File upload + Excel import | Magic-byte validation, path traversal prevention, CSV injection, batch import | 11 |
| **8** | Audit trail + Notifications + Hardening | Immutable audit, OWASP scan, HSTS headers, circuit breakers, encrypted backups | 12–13 |
| **9** | Testing + Packaging + Deployment | 80% coverage, security tests, Docker, `.exe` installer, deployment checklist | 14–16 |

---

**Total Duration: ~16 weeks (4 months)**

**End Result:** A fully functional, security-hardened CRM desktop application with a `.exe` installer for Windows and a Dockerized backend — ready for production use or academic submission.
