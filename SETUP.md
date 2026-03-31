# CRM TempO — Setup & Deployment Guide

Complete step-by-step instructions to run the CRM application locally, with Docker, and on GitHub.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Local Development Setup](#2-local-development-setup)
3. [Docker Deployment](#3-docker-deployment)
4. [Running the Desktop App](#4-running-the-desktop-app)
5. [GitHub CI/CD](#5-github-cicd)
6. [Deploying to a Cloud Server](#6-deploying-to-a-cloud-server)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| **Java JDK** | 17+ | Compile & run both backend and desktop |
| **Maven** | 3.8+ | Build tool |
| **PostgreSQL** | 16+ | Database (local dev only — Docker provides its own) |
| **Redis** | 7+ | Cache (optional — app works without it) |
| **Docker** | 24+ | Containerized deployment |
| **Docker Compose** | v2+ | Multi-service orchestration |
| **Git** | 2.30+ | Source control |

### Install checklist

```bash
# Verify installations
java -version          # Should show 17+
mvn -version           # Should show 3.8+
psql --version         # Should show 16+ (local dev only)
docker --version       # Should show 24+
docker compose version # Should show v2+
```

---

## 2. Local Development Setup

### Step 1: Clone the repository

```bash
git clone https://github.com/azharali0/CRM-TempO-Project.git
cd CRM-TempO-Project
```

### Step 2: Set up PostgreSQL

```bash
# Create database and user
sudo -u postgres psql -c "CREATE USER crm_user WITH PASSWORD 'crm_pass';"
sudo -u postgres psql -c "CREATE DATABASE crm_db OWNER crm_user;"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE crm_db TO crm_user;"
```

**Verify connection:**
```bash
psql -h localhost -U crm_user -d crm_db -c "SELECT 1;"
```

### Step 3: Set up Redis (optional)

```bash
# Ubuntu/Debian
sudo apt install redis-server
sudo systemctl start redis

# macOS
brew install redis
brew services start redis

# Verify
redis-cli ping    # Should return PONG
```

> **Note:** The app works without Redis. Report caching will be skipped.

### Step 4: Configure environment

```bash
cd crm-backend

# Copy example config
cp .env.example .env

# Edit with your local values
# Key settings to change:
#   DB_URL=jdbc:postgresql://localhost:5432/crm_db
#   DB_USERNAME=crm_user
#   DB_PASSWORD=crm_pass
#   JWT_SECRET=<any-string-at-least-32-characters-long>
```

### Step 5: Build and run the backend

```bash
cd crm-backend

# Download dependencies and compile
mvn clean compile

# Run all tests (91 tests)
mvn test

# Start the application
mvn spring-boot:run
```

**Verify it's running:**
```bash
# Health check
curl http://localhost:8080/api/system/health

# Version
curl http://localhost:8080/api/system/version

# Swagger docs (open in browser)
open http://localhost:8080/swagger-ui.html
```

### Step 6: Register your first user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin User",
    "email": "admin@example.com",
    "password": "Admin123!@#",
    "role": "ADMIN"
  }'
```

### Step 7: Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "Admin123!@#"
  }'
# Returns: { "accessToken": "eyJ...", "refreshToken": "...", "role": "ADMIN" }
```

Use the `accessToken` in subsequent requests:
```bash
curl -H "Authorization: Bearer <accessToken>" \
  http://localhost:8080/api/reports/dashboard
```

---

## 3. Docker Deployment

### Step 1: Create environment file

```bash
cd CRM-TempO-Project   # repository root

# Copy and edit the environment file
cp .env.example .env

# IMPORTANT: change these values in .env:
#   DB_PASSWORD=<strong-password>
#   JWT_SECRET=<random-string-at-least-32-chars>
```

### Step 2: Start all services

```bash
docker compose up -d
```

This starts three containers:
| Service | Port | Description |
|---------|------|-------------|
| **app** | 8080 | Spring Boot API |
| **db** | 5432 | PostgreSQL 16 |
| **redis** | 6379 | Redis 7 |

### Step 3: Verify

```bash
# Check all containers are running
docker compose ps

# Check backend logs
docker compose logs app

# Health check
curl http://localhost:8080/api/system/health

# Swagger docs
open http://localhost:8080/swagger-ui.html
```

### Step 4: Stop / Restart

```bash
# Stop all
docker compose down

# Stop and remove data volumes (fresh start)
docker compose down -v

# Rebuild after code changes
docker compose up -d --build
```

---

## 4. Running the Desktop App

> **Requirement:** The backend must be running (local or Docker) at `http://localhost:8080`.

### Build and run

```bash
cd crm-desktop

# Compile
mvn clean compile

# Run desktop tests (40 tests)
mvn test

# Launch the JavaFX application
mvn javafx:run
```

### Desktop screens

| Screen | Description |
|--------|-------------|
| **Login** | Email + password authentication |
| **Dashboard** | KPI cards + bar/pie/line charts |
| **Customers** | List → Detail → Edit forms, document upload |
| **Leads** | Kanban pipeline board + detail view |
| **Tasks** | Task list + create/edit form |
| **Interactions** | Customer interaction timeline |
| **Reports** | Conversion, sales-by-rep, monthly trends + PDF export |
| **Import** | Excel/CSV file import |
| **Notifications** | Bell icon with unread count (polls every 30s) |

### Build Windows .exe installer (Windows only)

```bash
cd crm-desktop
build-installer.bat
# Output: target/installer/CRM Desktop-1.0.0.exe
```

---

## 5. GitHub CI/CD

The project includes a GitHub Actions workflow (`.github/workflows/ci.yml`) that runs automatically on every push and pull request to `main`.

### What CI does

| Job | Steps |
|-----|-------|
| **backend** | Checkout → JDK 17 → `mvn clean compile test` (91 tests) |
| **desktop** | Checkout → JDK 17 → `mvn clean compile test` (40 tests) |

### Running CI manually

Push to `main` or open a pull request — CI runs automatically.

```bash
git add .
git commit -m "my changes"
git push origin main
```

Then check the **Actions** tab on GitHub to see the build results.

---

## 6. Deploying to a Cloud Server

### Option A: Docker on a VPS (DigitalOcean, AWS EC2, etc.)

```bash
# 1. SSH into your server
ssh user@your-server-ip

# 2. Install Docker
curl -fsSL https://get.docker.com | sh

# 3. Clone the repo
git clone https://github.com/azharali0/CRM-TempO-Project.git
cd CRM-TempO-Project

# 4. Configure environment
cp .env.example .env
nano .env   # Set strong passwords and JWT secret

# 5. Start
docker compose up -d

# 6. Verify
curl http://localhost:8080/api/system/health
```

### Option B: Railway / Render (Free Tier)

1. Fork the repository on GitHub
2. Connect your GitHub account to Railway/Render
3. Create a new project from `crm-backend/`
4. Add a PostgreSQL add-on
5. Set environment variables (see `.env.example`)
6. Deploy — it auto-detects the Dockerfile

---

## 7. Troubleshooting

### Backend won't start

| Symptom | Fix |
|---------|-----|
| `Connection refused` to PostgreSQL | Start PostgreSQL: `sudo systemctl start postgresql` |
| `FATAL: database "crm_db" does not exist` | Create it: `createdb crm_db` |
| `FATAL: role "postgres" does not exist` | Create user: `sudo -u postgres createuser --superuser $USER` |
| `Flyway migration failed` | Drop and recreate DB: `dropdb crm_db && createdb crm_db` |
| `JWT_SECRET too short` | Set a string with at least 32 characters in `.env` |
| `Port 8080 already in use` | Kill it: `lsof -ti:8080 \| xargs kill` or change `SERVER_PORT` |

### Redis issues

| Symptom | Fix |
|---------|-----|
| `Cannot connect to Redis` | Start Redis: `sudo systemctl start redis` |
| App works but reports are slow | Redis is optional — enable for caching |

### Docker issues

| Symptom | Fix |
|---------|-----|
| `build: .` fails | Fixed in latest version — uses `build: context: ./crm-backend` |
| `Cannot find .env` | Copy it: `cp .env.example .env` |
| DB not ready when app starts | docker-compose uses health checks — wait 30 seconds |
| Need to reset everything | `docker compose down -v` then `docker compose up -d` |

### Desktop app issues

| Symptom | Fix |
|---------|-----|
| `Connection refused` on login | Start the backend first at port 8080 |
| JavaFX not found | Ensure JDK 17+ is installed (not just JRE) |
| `mvn javafx:run` fails | Run `mvn clean compile` first |
| White screen on startup | Check that backend is running and accessible |

### Test failures

```bash
# Run backend tests with verbose output
cd crm-backend && mvn test -X

# Run a specific test class
mvn test -Dtest=AuthServiceTest

# Run desktop tests
cd crm-desktop && mvn test
```

### Account lockout

If you get locked out after 5 failed login attempts, wait 30 minutes or restart the backend (in-memory lockout resets).

### Common API errors

| HTTP Code | Meaning | Fix |
|-----------|---------|-----|
| 401 | Token expired | Call `/api/auth/refresh` with your refresh token |
| 403 | Access denied | Your role doesn't have permission for this endpoint |
| 409 | Conflict | Optimistic locking — re-fetch the record and retry |
| 429 | Rate limited | Wait 1 minute (5 requests/min on auth endpoints) |

---

## Quick Reference

```bash
# ── Local Backend ──────────────────────────
cd crm-backend && mvn spring-boot:run

# ── Local Desktop ──────────────────────────
cd crm-desktop && mvn javafx:run

# ── Docker (full stack) ───────────────────
cp .env.example .env       # first time only
docker compose up -d

# ── Run all tests ─────────────────────────
cd crm-backend && mvn test    # 91 tests
cd crm-desktop && mvn test    # 40 tests

# ── Rebuild after changes ─────────────────
docker compose up -d --build
```
