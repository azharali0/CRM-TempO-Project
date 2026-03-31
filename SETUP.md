# CRM TempO — Setup & Deployment Guide

Complete step-by-step instructions to run the CRM application on **Windows**, **macOS**, **Linux**, with **Docker**, and on **online cloud platforms**.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Windows PC Setup (From Cloning to Usage)](#2-windows-pc-setup-from-cloning-to-usage)
3. [macOS / Linux Setup](#3-macos--linux-setup)
4. [Docker Deployment (Any OS)](#4-docker-deployment-any-os)
5. [Running the Desktop App](#5-running-the-desktop-app)
6. [Online Deployment (Cloud Platforms)](#6-online-deployment-cloud-platforms)
7. [GitHub CI/CD](#7-github-cicd)
8. [Using the Application](#8-using-the-application)
9. [Troubleshooting](#9-troubleshooting)

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

---

## 2. Windows PC Setup (From Cloning to Usage)

This section walks you through **every step** to run the CRM application on a Windows PC.

### Step 1: Install Required Software

#### Install Git
1. Download from https://git-scm.com/download/win
2. Run the installer — use all default settings
3. Verify: open **Command Prompt** or **PowerShell** and run:
   ```cmd
   git --version
   ```

#### Install Java JDK 17
1. Download from https://adoptium.net/ — choose **Windows x64 .msi** (Temurin JDK 17)
2. Run the installer — **check "Set JAVA_HOME variable"** during install
3. Verify:
   ```cmd
   java -version
   javac -version
   ```

#### Install Maven
1. Download from https://maven.apache.org/download.cgi — choose the **Binary zip archive**
2. Extract to `C:\maven` (so you have `C:\maven\bin\mvn.cmd`)
3. Add to PATH:
   - Press **Win + R** → type `sysdm.cpl` → **Advanced** → **Environment Variables**
   - Under **System variables**, find `Path` → **Edit** → **New** → add `C:\maven\bin`
4. Verify (open a **new** Command Prompt):
   ```cmd
   mvn -version
   ```

#### Install PostgreSQL
1. Download from https://www.postgresql.org/download/windows/ — use the **EDB installer**
2. Run the installer:
   - Set a password for the `postgres` user (remember this!)
   - Keep default port **5432**
   - Finish the install (you can skip Stack Builder)
3. The installer adds `psql` to your PATH automatically

#### Install Redis (Optional)
> The app works without Redis — report caching will just be skipped.

1. Download from https://github.com/tporadowski/redis/releases — choose the `.msi` installer
2. Run the installer with default settings
3. Redis starts automatically as a Windows service

### Step 2: Clone the Repository

Open **Command Prompt** or **PowerShell**:

```cmd
cd %USERPROFILE%\Desktop
git clone https://github.com/azharali0/CRM-TempO-Project.git
cd CRM-TempO-Project
```

### Step 3: Set Up the Database

Open **Command Prompt** and run:

```cmd
psql -U postgres
```

Enter the password you set during PostgreSQL installation, then run these SQL commands:

```sql
CREATE USER crm_user WITH PASSWORD 'crm_pass';
CREATE DATABASE crm_db OWNER crm_user;
GRANT ALL PRIVILEGES ON DATABASE crm_db TO crm_user;
\q
```

**Verify the connection:**
```cmd
psql -h localhost -U crm_user -d crm_db -c "SELECT 1;"
```
(Enter password: `crm_pass`)

### Step 4: Configure Environment

```cmd
cd crm-backend
copy .env.example .env
notepad .env
```

In Notepad, update these values:
```
DB_URL=jdbc:postgresql://localhost:5432/crm_db
DB_USERNAME=crm_user
DB_PASSWORD=crm_pass
JWT_SECRET=my_super_secret_jwt_key_that_is_at_least_32_chars_long
```

Save and close Notepad.

### Step 5: Build and Run the Backend

```cmd
cd crm-backend

REM Download dependencies and compile
mvn clean compile

REM Run all tests (91 tests should pass)
mvn test

REM Start the application
mvn spring-boot:run
```

> **Leave this window open!** The backend runs until you press Ctrl+C.

### Step 6: Verify It Works

Open a **new** Command Prompt window (keep the backend running):

```cmd
REM Health check
curl http://localhost:8080/api/system/health

REM Version
curl http://localhost:8080/api/system/version
```

Or open your browser and go to:
- **Swagger API docs:** http://localhost:8080/swagger-ui.html
- **Health check:** http://localhost:8080/api/system/health

### Step 7: Register Your First User

In the new Command Prompt:

```cmd
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"name\":\"Admin User\",\"email\":\"admin@example.com\",\"password\":\"Admin123!@#\",\"role\":\"ADMIN\"}"
```

### Step 8: Login

```cmd
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@example.com\",\"password\":\"Admin123!@#\"}"
```

This returns a JSON response with your `accessToken`. Copy it and use it in API calls:

```cmd
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE" http://localhost:8080/api/reports/dashboard
```

### Step 9: Run the Desktop App (Optional)

Open a **third** Command Prompt (keep the backend running):

```cmd
cd %USERPROFILE%\Desktop\CRM-TempO-Project\crm-desktop
mvn clean compile
mvn javafx:run
```

This launches the JavaFX desktop GUI. Login with the email/password you registered above.

### Step 10: Build a Windows .exe Installer (Optional)

```cmd
cd crm-desktop
build-installer.bat
```

The installer is created at `target\installer\CRM Desktop-1.0.0.exe`.

---

## 3. macOS / Linux Setup

### Step 1: Install prerequisites

**macOS (Homebrew):**
```bash
brew install openjdk@17 maven postgresql@16 redis git
brew services start postgresql@16
brew services start redis
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk maven postgresql redis-server git
sudo systemctl start postgresql
sudo systemctl start redis
```

**Fedora/RHEL:**
```bash
sudo dnf install java-17-openjdk-devel maven postgresql-server redis git
sudo postgresql-setup --initdb
sudo systemctl start postgresql
sudo systemctl start redis
```

**Verify installations:**
```bash
java -version          # Should show 17+
mvn -version           # Should show 3.8+
psql --version         # Should show 16+
redis-cli ping         # Should return PONG
```

### Step 2: Clone the repository

```bash
git clone https://github.com/azharali0/CRM-TempO-Project.git
cd CRM-TempO-Project
```

### Step 3: Set up PostgreSQL

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

### Step 4: Configure environment

```bash
cd crm-backend
cp .env.example .env

# Edit with your local values
nano .env   # or: vim .env / code .env

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

# Swagger docs (open in browser)
open http://localhost:8080/swagger-ui.html    # macOS
xdg-open http://localhost:8080/swagger-ui.html  # Linux
```

### Step 6: Register and login

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin User",
    "email": "admin@example.com",
    "password": "Admin123!@#",
    "role": "ADMIN"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "Admin123!@#"
  }'
```

Use the `accessToken` in subsequent requests:
```bash
curl -H "Authorization: Bearer <accessToken>" \
  http://localhost:8080/api/reports/dashboard
```

---

## 4. Docker Deployment (Any OS)

Docker is the **easiest way** to run the full application — it handles PostgreSQL, Redis, and the backend in one command.

### Install Docker

**Windows:** Download and install [Docker Desktop](https://www.docker.com/products/docker-desktop/) — it includes Docker Compose.

**macOS:** `brew install --cask docker` or download from https://www.docker.com/products/docker-desktop/

**Linux:**
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER   # then log out/in
```

### Step 1: Clone and configure

```bash
git clone https://github.com/azharali0/CRM-TempO-Project.git
cd CRM-TempO-Project

# Create environment file
cp .env.example .env
```

Edit `.env` and change these values:
```
DB_PASSWORD=a_strong_password_here
JWT_SECRET=a_random_string_at_least_32_characters_long
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
```

Or open in your browser:
- **Swagger docs:** http://localhost:8080/swagger-ui.html
- **Health:** http://localhost:8080/api/system/health

### Step 4: Register and start using

```bash
# Register an admin user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin","email":"admin@example.com","password":"Admin123!@#","role":"ADMIN"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin123!@#"}'
```

### Step 5: Stop / Restart

```bash
# Stop all
docker compose down

# Stop and remove data volumes (fresh start)
docker compose down -v

# Rebuild after code changes
docker compose up -d --build
```

---

## 5. Running the Desktop App

> **Requirement:** The backend must be running (local or Docker) at `http://localhost:8080`.

### Build and run

**Windows:**
```cmd
cd crm-desktop
mvn clean compile
mvn javafx:run
```

**macOS / Linux:**
```bash
cd crm-desktop
mvn clean compile
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

### Run desktop tests

```bash
cd crm-desktop
mvn test    # 40 tests
```

### Build Windows .exe installer (Windows only)

```cmd
cd crm-desktop
build-installer.bat
REM Output: target\installer\CRM Desktop-1.0.0.exe
```

---

## 6. Online Deployment (Cloud Platforms)

Deploy the CRM backend online so it's accessible from the internet — use any of these options:

### Option A: Railway (Free Tier — Recommended for Quick Deploy)

[Railway](https://railway.app/) gives you a free hosted app with PostgreSQL.

1. **Fork the repository** on GitHub → https://github.com/azharali0/CRM-TempO-Project
2. Go to https://railway.app/ and **sign in with GitHub**
3. Click **"New Project"** → **"Deploy from GitHub Repo"** → select your fork
4. Railway auto-detects the Dockerfile in `crm-backend/`. If it asks for the root directory, enter: `crm-backend`
5. **Add a PostgreSQL database:**
   - Click **"New"** → **"Database"** → **"PostgreSQL"**
   - Railway auto-generates `DATABASE_URL`
6. **Set environment variables** (click on your service → **Variables** tab):
   ```
   DB_URL=<Railway provides this as DATABASE_URL — copy the JDBC format>
   DB_USERNAME=<from Railway PostgreSQL>
   DB_PASSWORD=<from Railway PostgreSQL>
   JWT_SECRET=my_super_secret_jwt_key_that_is_at_least_32_chars
   REDIS_HOST=localhost
   SERVER_PORT=8080
   ```
   > **Tip:** Railway provides `DATABASE_URL` in the format `postgresql://user:pass@host:port/db`. Convert to JDBC: `jdbc:postgresql://host:port/db`
7. **Deploy** — Railway builds and starts your app automatically
8. Railway gives you a public URL like `https://your-app.up.railway.app`
9. **Test:** Visit `https://your-app.up.railway.app/swagger-ui.html`

### Option B: Render (Free Tier)

[Render](https://render.com/) is another easy option with a free PostgreSQL database.

1. **Fork the repository** on GitHub
2. Go to https://render.com/ and **sign in with GitHub**
3. Click **"New"** → **"Web Service"**
4. Connect your forked repository
5. Configure:
   - **Root Directory:** `crm-backend`
   - **Environment:** `Docker`
   - **Region:** Choose the nearest to you
6. **Add a PostgreSQL database:**
   - Click **"New"** → **"PostgreSQL"** → create a free instance
   - Copy the **Internal Database URL**
7. **Add environment variables** (on the web service settings):
   ```
   DB_URL=jdbc:postgresql://<internal-host>:5432/<db-name>
   DB_USERNAME=<from Render PostgreSQL>
   DB_PASSWORD=<from Render PostgreSQL>
   JWT_SECRET=my_super_secret_jwt_key_that_is_at_least_32_chars
   REDIS_HOST=localhost
   SERVER_PORT=8080
   ```
8. **Deploy** — Render builds from the Dockerfile and starts your app
9. Your app is live at `https://your-app.onrender.com`
10. **Test:** Visit `https://your-app.onrender.com/swagger-ui.html`

### Option C: Docker on a VPS (DigitalOcean, AWS EC2, Azure VM, etc.)

For full control, deploy to any cloud server with Docker:

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

# 5. Start all services
docker compose up -d

# 6. Verify
curl http://localhost:8080/api/system/health

# 7. (Optional) Set up a reverse proxy with nginx for HTTPS
```

> **Cost:** DigitalOcean droplets start at $4/month, AWS EC2 free tier is available for 12 months.

### Option D: GitHub Codespaces (Run in Browser)

If you just want to **try the backend** without installing anything:

1. Go to https://github.com/azharali0/CRM-TempO-Project
2. Click the green **"Code"** button → **"Codespaces"** → **"Create codespace on main"**
3. Wait for the environment to load (VS Code in browser)
4. In the terminal:
   ```bash
   cd crm-backend
   mvn clean compile test    # Run all 91 tests
   ```
   > **Note:** You'll need a PostgreSQL service for `mvn spring-boot:run`. Codespaces is best for browsing code and running tests.

---

## 7. GitHub CI/CD

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

## 8. Using the Application

Once the backend is running (locally, Docker, or online), here's how to use it:

### API Workflow

```
1. Register → POST /api/auth/register
2. Login    → POST /api/auth/login       → get accessToken
3. Use API  → GET/POST/PUT with "Authorization: Bearer <accessToken>"
4. Refresh  → POST /api/auth/refresh     → when token expires (15 min)
5. Logout   → POST /api/auth/logout
```

### Example: Full Workflow via curl

```bash
# Set your base URL (change for online deployment)
BASE=http://localhost:8080

# 1. Register
curl -X POST $BASE/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin","email":"admin@test.com","password":"Admin123!@#","role":"ADMIN"}'

# 2. Login and save the token
TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"Admin123!@#"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# 3. Create a customer
curl -X POST $BASE/api/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme Corp","email":"contact@acme.com","phone":"555-0100","company":"Acme"}'

# 4. View dashboard
curl -H "Authorization: Bearer $TOKEN" $BASE/api/reports/dashboard

# 5. List your notifications
curl -H "Authorization: Bearer $TOKEN" $BASE/api/notifications/my
```

### Using Swagger UI (Easiest)

1. Open http://localhost:8080/swagger-ui.html in your browser
2. Click **POST /api/auth/login** → **Try it out** → enter credentials → **Execute**
3. Copy the `accessToken` from the response
4. Click the **Authorize 🔒** button at the top → paste `Bearer <your-token>` → **Authorize**
5. Now you can try any API endpoint directly from the browser!

### Desktop App Usage

1. Start the backend (any method above)
2. Run `mvn javafx:run` in `crm-desktop/`
3. Login with the credentials you registered
4. Use the sidebar to navigate: Dashboard, Customers, Leads, Tasks, Reports, etc.

---

## 9. Troubleshooting

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

# ── Useful URLs ───────────────────────────
# Swagger UI:  http://localhost:8080/swagger-ui.html
# Health:      http://localhost:8080/api/system/health
# Version:     http://localhost:8080/api/system/version
```
