# TALOMS — Traditional Authority Land & Occupancy Management System

<div align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![PostGIS](https://img.shields.io/badge/PostGIS-3.4-4B8BBE?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-Private-red?style=for-the-badge)

**A secure, GIS-enabled land administration platform for Traditional Authorities in South Africa.**

[Overview](#overview) · [Architecture](#architecture) · [Prerequisites](#prerequisites) · [Getting Started](#getting-started) · [Project Structure](#project-structure) · [API Docs](#api-documentation) · [Contributing](#contributing)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [1. Clone the Repository](#1-clone-the-repository)
  - [2. Configure the Database (Docker)](#2-configure-the-database-docker)
  - [3. Fix Docker Authentication on Windows](#3-fix-docker-authentication-on-windows-critical)
  - [4. Configure application.properties](#4-configure-applicationproperties)
  - [5. Open in IntelliJ IDEA](#5-open-in-intellij-idea)
  - [6. Run the Application](#6-run-the-application)
  - [7. Verify the Application](#7-verify-the-application)
- [Project Structure](#project-structure)
- [Database Migrations](#database-migrations)
- [User Roles](#user-roles)
- [Default Credentials](#default-credentials)
- [API Documentation](#api-documentation)
- [Environment Profiles](#environment-profiles)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## Overview

TALOMS replaces paper-based Permission to Occupy (PTO) registers, physical maps, spreadsheets, and manual filing systems used by Traditional Authorities in South Africa with a single authoritative digital platform.

The system serves Traditional Councils, Chiefs, Headmen, Land Officers, and administrative offices — enabling them to digitally manage land allocations, record GPS boundaries, maintain household records, and produce official reports in compliance with South African legislation including **POPIA** (Protection of Personal Information Act).

---

## Features

| Module | Description |
|---|---|
| **Traditional Authority Management** | Register and manage Chiefs, Headmen, and tribal jurisdictions |
| **Permission to Occupy (PTO)** | Full PTO lifecycle — creation, approval, revocation, expiry |
| **PTO Legal Compliance** | Enforces SA PTO legislation: TA allocation letter, site sketch, ID verification |
| **Land Parcel Management** | Stand demarcation, GPS boundaries, automatic area calculation |
| **GIS Mapping** | Interactive Leaflet map with OpenStreetMap and PostGIS spatial queries |
| **Household Management** | Household records linked to parcels and PTOs |
| **Resident Management** | Resident records with SA ID validation and relationship tracking |
| **Business Occupancy** | Commercial stand registration and management |
| **Document Management** | Upload and storage of PTO certificates, ID copies, survey drawings |
| **Reporting** | PDF and Excel reports — PTO, parcel, population, land utilisation |
| **Audit Trail** | Immutable audit log for every action — POPIA compliance |
| **Dashboard** | KPI summary for the logged-in user's authority scope |

---

## Architecture

TALOMS uses a **Modular Monolith** architecture — one deployable Spring Boot JAR internally structured as independent business domains with strict boundaries.

```
┌─────────────────────────────────────────────────────────┐
│                  TALOMS Application                      │
│                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ security │ │   pto    │ │  parcel  │ │   gis    │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │household │ │ resident │ │ document │ │reporting │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│                                                          │
│              Spring Events (async)                       │
│         PostgreSQL 16 + PostGIS 3.4                      │
└─────────────────────────────────────────────────────────┘
```

Each module follows a **four-layer Clean Architecture** pattern:

```
module/
├── presentation/      ← Controllers (HTTP layer)
├── application/
│   ├── service/       ← Service interfaces + implementations
│   └── dto/           ← Request and response DTOs
├── domain/
│   ├── entity/        ← JPA entities and enums
│   ├── event/         ← Spring domain events
│   ├── repository/    ← Repository port interfaces
│   └── rule/          ← Business rules
└── infrastructure/
    ├── repository/    ← JPA repositories + adapters
    └── mapper/        ← MapStruct mappers
```

**Design Principles:**
- Controllers depend on **service interfaces only** — never implementations
- Services depend on **repository port interfaces only** — never JPA directly
- Cross-module communication via **Spring ApplicationEvents** — loose coupling
- All repositories are **private to their module** — never accessed externally

---

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 LTS (OpenJDK 21) |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security 7 + JWT (JJWT 0.12.6) |
| Persistence | Spring Data JPA + Hibernate 7 |
| Database | PostgreSQL 16 |
| Spatial Extension | PostGIS 3.4 |
| DB Migrations | Flyway 12 |
| Frontend | Thymeleaf 3 + Bootstrap 5 + Leaflet 1.9 |
| Object Mapping | MapStruct 1.5 + Lombok |
| Build Tool | Apache Maven 3.9 |
| Containerisation | Docker + Docker Compose |
| IDE (recommended) | IntelliJ IDEA 2024+ (Community or Ultimate) |

---

## Prerequisites

Ensure the following are installed before setting up the project:

| Tool | Version | Download |
|---|---|---|
| Git | Latest | https://git-scm.com |
| OpenJDK | 21 LTS | https://adoptium.net |
| Apache Maven | 3.9+ | https://maven.apache.org |
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
| IntelliJ IDEA | 2024+ | https://www.jetbrains.com/idea/download |

> **Windows users:** Docker Desktop requires WSL2.
> Run the following in PowerShell as Administrator before installing Docker Desktop:
> ```powershell
> wsl --install
> ```
> Restart your machine after WSL2 installation completes.

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/taloms.git
cd taloms
```

---

### 2. Configure the Database (Docker)

TALOMS uses **PostgreSQL 16 with PostGIS 3.4** running in Docker.

> **Important:** Use `postgis/postgis:16-3.4` — not version 17.
> PostgreSQL 17 uses `scram-sha-256` authentication by default which conflicts
> with the JDBC driver over the Docker bridge network on Windows.

The `docker-compose.yml` is already configured correctly in the repository.
Start the database container:

```bash
docker compose up -d
```

Verify the container is healthy:

```bash
docker compose ps
```

Expected output:
```
NAME        IMAGE                    STATUS              PORTS
taloms_db   postgis/postgis:16-3.4   Up X seconds (healthy)   0.0.0.0:5432->5432/tcp
```

Wait until STATUS shows `(healthy)` before proceeding.

---

### 3. Fix Docker Authentication on Windows (CRITICAL)

> **This step is required on Windows with Docker Desktop.**
> On Linux or macOS you can skip to Step 4.

The PostgreSQL 16 Docker image requires a one-time authentication fix on Windows
due to how Docker Desktop routes TCP connections through the WSL2 bridge.

**Run these commands in PowerShell — in this exact order:**

**Step A — Rewrite pg_hba.conf to use trust authentication:**
```powershell
docker exec taloms_db bash -c "cat > /var/lib/postgresql/data/pg_hba.conf << 'PGEOF'
local all all trust
host all all all trust
PGEOF"
```

**Step B — Reload PostgreSQL configuration:**
```powershell
docker exec taloms_db psql -U taloms_user -d taloms_db -c "SELECT pg_reload_conf();"
```

**Step C — Set the password using md5 encryption:**
```powershell
docker exec taloms_db psql -U taloms_user -d taloms_db -c "SET password_encryption='md5'; ALTER USER taloms_user WITH PASSWORD 'taloms_pass';"
```

**Step D — Verify the password is stored as md5:**
```powershell
docker exec taloms_db psql -U taloms_user -d taloms_db -c "SELECT rolpassword FROM pg_authid WHERE rolname='taloms_user';"
```

The output must start with `md5` — not `SCRAM-SHA-256`:
```
             rolpassword
-------------------------------------
 md5ed7baac24b6087dce64001ee0dd52678
```

**Step E — Verify no password prompt on connection:**
```powershell
docker exec taloms_db psql -U taloms_user -d taloms_db -c "SELECT current_user;" -w
```

Must return `taloms_user` without prompting for a password.

> **Note:** These changes are stored in the Docker volume.
> If you run `docker compose down -v` you must repeat Steps A–E on the new container.
> To avoid this, never use `docker compose down -v` unless you need to wipe all data.
> Use `docker compose down` (without `-v`) to stop and restart safely.

---

### 4. Configure application.properties

Open `src/main/resources/application.properties`.

Verify it contains exactly the following — do not add `sslmode` or `gssEncMode` parameters:

```properties
# ── DataSource ────────────────────────────────────────────────────────────────
spring.datasource.url=jdbc:postgresql://localhost:5432/taloms_db
spring.datasource.username=taloms_user
spring.datasource.password=taloms_pass
spring.datasource.driver-class-name=org.postgresql.Driver

# ── JPA / Hibernate ───────────────────────────────────────────────────────────
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# ── Flyway ────────────────────────────────────────────────────────────────────
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# ── Thymeleaf ─────────────────────────────────────────────────────────────────
spring.thymeleaf.cache=false

# ── Server ────────────────────────────────────────────────────────────────────
server.port=8080

# ── Logging ───────────────────────────────────────────────────────────────────
logging.level.za.co.taloms=DEBUG
logging.level.org.springframework.security=INFO
logging.level.org.flywaydb=INFO

# ── JWT ───────────────────────────────────────────────────────────────────────
taloms.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
taloms.jwt.expiration=28800000
```

---

### 5. Open in IntelliJ IDEA

1. Open IntelliJ IDEA
2. **File → Open** → select the `taloms` folder
3. Click **Trust Project** when prompted
4. Wait for Maven to download all dependencies (watch the progress bar at the bottom — this takes 2–5 minutes on first open)

**Required IntelliJ settings:**

**Enable Annotation Processing (required for Lombok and MapStruct):**
```
Settings → Build, Execution, Deployment → Compiler → Annotation Processors
→ tick "Enable annotation processing" → OK
```

**Install the Lombok plugin:**
```
Settings → Plugins → search "Lombok" → Install → Restart IntelliJ
```

**Verify JDK 21 is selected:**
```
File → Project Structure → Project → SDK → must show "21"
```

If JDK 21 is not listed:
```
Add SDK → Download JDK → Version: 21 (Eclipse Temurin) → Download
```

---

### 6. Run the Application

**Option A — From IntelliJ:**

Click the green **Run** button or press `Shift + F10`.

**Option B — From the terminal:**

```bash
mvn clean spring-boot:run
```

---

### 7. Verify the Application

A successful startup looks like this in the console:

```
HikariPool-1 - Starting...
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
Database: jdbc:postgresql://localhost:5432/taloms_db (PostgreSQL 16.4)
Successfully applied N migrations to schema "public"
Started TalomsApplication in X.XXX seconds
```

Open your browser:

```
http://localhost:8080
```

You will see the TALOMS login page. Log in with the default admin credentials:

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `Admin@1234` |

> **Security notice:** Change the admin password immediately after first login.

---

## Project Structure

```
taloms/
│
├── docker-compose.yml                     ← PostgreSQL 16 + PostGIS container
├── Dockerfile                             ← Production Docker image
├── pom.xml                                ← Maven build and dependencies
├── README.md
│
└── src/
    ├── main/
    │   ├── java/za/co/taloms/
    │   │   │
    │   │   ├── TalomsApplication.java     ← Spring Boot entry point
    │   │   │
    │   │   ├── common/                    ← Shared exceptions, DTOs, constants
    │   │   │   ├── ApiResponse.java
    │   │   │   ├── ApplicationConstants.java
    │   │   │   ├── BusinessValidationException.java
    │   │   │   ├── DuplicateRecordException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── PageResponse.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── Roles.java
    │   │   │
    │   │   ├── configuration/             ← Spring configuration beans
    │   │   ├── security/                  ← JWT, authentication, users, roles
    │   │   ├── traditionalauthority/      ← Traditional Authority management
    │   │   ├── village/                   ← Village management
    │   │   ├── pto/                       ← Permission to Occupy (core module)
    │   │   ├── parcel/                    ← Land parcel and boundary management
    │   │   ├── gis/                       ← GIS processing and spatial services
    │   │   ├── household/                 ← Household records
    │   │   ├── resident/                  ← Resident personal records
    │   │   ├── businessoccupancy/         ← Business stand occupancy
    │   │   ├── document/                  ← File upload and storage
    │   │   ├── reporting/                 ← PDF and Excel report generation
    │   │   ├── audit/                     ← Audit trail
    │   │   ├── notification/              ← Notification services (Phase 2)
    │   │   └── dashboard/                 ← Dashboard KPI summary
    │   │
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       ├── application-prod.properties
    │       ├── db/migration/              ← Flyway SQL migration scripts
    │       │   ├── V1__init_schema.sql
    │       │   ├── V2__create_authority_tables.sql
    │       │   ├── V3__create_security_tables.sql
    │       │   ├── V4__create_pto_tables.sql
    │       │   └── V5__seed_roles_and_admin.sql
    │       ├── templates/                 ← Thymeleaf HTML templates
    │       └── static/                    ← CSS, JS, images
    │
    └── test/
        └── java/za/co/taloms/            ← Unit and integration tests
```

---

## Database Migrations

Schema changes are managed by **Flyway**. Migration scripts live in:

```
src/main/resources/db/migration/
```

### Naming Convention

```
V{version}__{description}.sql
```

Examples:
```
V1__init_schema.sql
V2__create_authority_tables.sql
V6__add_parcel_geometry_index.sql
```

### Rules

- **Never modify an existing migration file** — Flyway checksums each file and will fail if you change one that has already been applied
- Always create a **new versioned file** for any schema change
- Migrations run automatically on application startup
- Use descriptive names — the description becomes the migration label in logs

---

## User Roles

| Role | Description | Key Permissions |
|---|---|---|
| `ROLE_SYSTEM_ADMIN` | Full system access | User management, all modules, system configuration |
| `ROLE_TA_ADMINISTRATOR` | TA office administrator | Approve/revoke PTOs, manage villages, all reports |
| `ROLE_LAND_OFFICER` | Field officer | Create parcels, demarcate stands, capture GPS |
| `ROLE_DATA_CAPTURER` | Office data entry | Capture household, resident, business records; upload documents |
| `ROLE_REPORT_VIEWER` | Read-only access | Search, view records, download reports |

---

## Default Credentials

| Role | Username | Password |
|---|---|---|
| System Administrator | `admin` | `Admin@1234` |

> Change this password immediately after first login in any environment.

---

## API Documentation

When the application is running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

The OpenAPI JSON specification is available at:

```
http://localhost:8080/v3/api-docs
```

All endpoints require a valid JWT Bearer token in the `Authorization` header:

```
Authorization: Bearer <your-token>
```

Obtain a token by calling the login endpoint:

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@1234"
}
```

---

## Environment Profiles

| Profile | Purpose | How to activate |
|---|---|---|
| `default` | Development — connects to local Docker database | No action needed |
| `dev` | Development with extra debug logging | `--spring.profiles.active=dev` |
| `prod` | Production — reads credentials from environment variables | `--spring.profiles.active=prod` |

**Run with a specific profile:**

```bash
# From Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# From JAR
java -jar target/taloms-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

**Production environment variables:**

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/taloms_db
export SPRING_DATASOURCE_USERNAME=taloms_user
export SPRING_DATASOURCE_PASSWORD=your-secure-password
export TALOMS_JWT_SECRET=your-256-bit-secret
```

---

## Troubleshooting

### Two processes listening on port 5432

**Symptom:** `netstat -ano | findstr :5432` shows two different PIDs.

**Cause:** A native PostgreSQL installation is running alongside Docker.

**Fix:** Stop the conflicting service:
```powershell
# Find the process
Get-Process -Id <PID>

# Stop the Windows PostgreSQL service
Stop-Service postgresql-x64-16

# Prevent it from auto-starting
Set-Service postgresql-x64-16 -StartupType Manual
```

---

### Password authentication failed for user "taloms_user"

**Cause:** The Docker volume has stale authentication configuration.

**Fix:** Follow [Step 3](#3-fix-docker-authentication-on-windows-critical) of the setup guide exactly.

If the problem persists after following Step 3, destroy and recreate the volume:

```powershell
docker compose down -v --remove-orphans
docker compose up -d
```

Then repeat Step 3 (all sub-steps A through E).

---

### PostGIS functions not found

**Cause:** The PostGIS extension was not enabled in the database.

**Fix:**
```powershell
docker exec taloms_db psql -U taloms_user -d taloms_db -c "CREATE EXTENSION IF NOT EXISTS postgis;"
docker exec taloms_db psql -U taloms_user -d taloms_db -c "SELECT PostGIS_Version();"
```

---

### Flyway migration checksum mismatch

**Cause:** An existing migration file was modified after it was applied.

**Fix:** Never modify migration files that have been applied. Create a new migration instead. If this happened in development:

```powershell
docker compose down -v --remove-orphans
docker compose up -d
```

Repeat Step 3, then restart the application. All migrations will be reapplied from scratch.

---

### Lombok annotations not resolving in IntelliJ

**Fix:**
```
Settings → Build, Execution, Deployment → Compiler → Annotation Processors
→ Enable annotation processing → OK
→ Build → Rebuild Project
```

---

### Application starts slowly (30+ seconds)

This is normal on first startup after a Docker volume is recreated. PostgreSQL takes time to recover if it was not shut down cleanly. Subsequent startups are fast (under 10 seconds).

---

### Port 8080 already in use

Another application is using port 8080. Either stop it or change the TALOMS port:

```properties
# In application.properties
server.port=8081
```

---

## Contributing

This is a private project. All contributions require approval from the project owner.

### Branch Naming Convention

```
feature/pto-approval-workflow
bugfix/parcel-area-calculation
hotfix/security-token-expiry
refactor/audit-event-listener
```

### Commit Message Convention

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
feat: add PTO approval email notification
fix: correct parcel area calculation for irregular polygons
refactor: extract validation logic into PTOBusinessRules
docs: update API documentation for GIS endpoints
test: add unit tests for SA ID number validation
```

### Workflow

```bash
# 1. Create a feature branch from main
git checkout -b feature/your-feature-name

# 2. Make your changes and commit
git add .
git commit -m "feat: description of your change"

# 3. Push and open a pull request
git push origin feature/your-feature-name
```

### Before Submitting a Pull Request

- [ ] Application starts cleanly with `mvn clean spring-boot:run`
- [ ] All existing tests pass with `mvn test`
- [ ] New business logic has unit tests
- [ ] No new Flyway migration files modify existing migrations
- [ ] No hardcoded credentials or secrets in code
- [ ] Code follows the four-layer module structure

---

## POPIA Compliance Notes

TALOMS handles personal information as defined by the **Protection of Personal Information Act (POPIA)**. The following measures are implemented:

- All personal information is access-controlled by role
- Every data access and modification is recorded in an immutable audit log
- Document access is logged per-download
- Passwords are stored using BCrypt (strength factor 12)
- JWT tokens expire after 8 hours
- Account lockout activates after 5 consecutive failed login attempts
- Data retention policies are enforced via Flyway archival scripts

---

## PTO Module — Legal Compliance & Digital Workflow

### Overview

The PTO module digitises the traditional Permission to Occupy issuance process used by Traditional Authorities in South Africa. It enforces legal and administrative requirements derived from:

- The **Constitution of South Africa** (Ss 211–212: recognition of traditional leadership)
- The **Traditional Leadership and Governance Framework Act 41 of 2003** (TLGFA)
- The **Traditional and Khoisan Leadership Act 3 of 2019** (TKLA)
- The **Spatial Planning and Land Use Management Act 16 of 2013** (SPLUMA)
- **KwaZulu-Natal Land Affairs (Permission to Occupy) Regulations 1994**
- **Interim Protection of Informal Land Rights Act 31 of 1996** (IPILRA)
- Relevant High Court judgments (e.g. *Ingonyama Trust / CASAC 2021*; *Ngwasheng v Kgomo 2023*)

### Digital Transformation Objectives

| Paper-Based Process | TALOMS Digital Equivalent | Benefit |
|---|---|---|
| Manual PTO register book | Centralised digital PTO records with search | Instant retrieval, audit trail |
| Hand-written TA allocation letters | Scanned/uploaded TA Allocation Letter with checksum | Tamper-evident, versioned storage |
| Paper site sketches | Digital Site Sketch / Plan uploads | Geospatially linked, retrievable |
| Delayed approval queues | Real-time status dashboard | Immediate visibility of pending/active PTOs |
| Duplicate stand allocations | Automated duplicate detection by parcel & ID | Prevents boundary disputes |
| Disconnected municipal/TA records | Integrated Traditional Authority → Village → Parcel hierarchy | Single source of truth |
| Manual POPIA compliance | Role-based access, document download logs, immutable audit trail | Automated compliance reporting |

### PTO Creation Workflow (Digitised)

```
┌─────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  1. Select      │───▶│  2. Enter Holder │───▶│  3. TA Allocation│
│     Parcel      │    │     Details      │    │     Metadata     │
│  (AVAILABLE)    │    │  (ID, contact)   │    │ (Allocated by,   │
│                 │    │                  │    │  date, area, ref)│
└─────────────────┘    └──────────────────┘    └──────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  6. Household   │◀───│  5. Approval     │◀───│  4. Upload        │
│     Creation    │    │  (TA Admin)      │    │  Documents        │
│  (after ACTIVE) │    │  Gates on docs   │    │  (TA Letter,      │
│                 │    │  present)        │    │   Site Sketch)    │
└─────────────────┘    └──────────────────┘    └──────────────────┘
```

### Supporting Document Requirements

A PTO **cannot be approved** unless the following documents are uploaded and active:

| Document | Type | Required For | Description |
|---|---|---|---|
| **ID / Passport Copy** | `ID_COPY` | All PTOs | SA ID or passport for identity verification |
| **TA Allocation Letter** | `TA_ALLOCATION_LETTER` | All PTOs | Traditional Authority recommendation confirming land allocation |
| **Site Sketch / Plan** | `SITE_SKETCH` | All PTOs | Stand boundaries, dimensions, north point, adjacent stands |
| **Community Resolution** | `COMMUNITY_RESOLUTION` | Agricultural & Business PTOs | Community meeting minutes/resolution (SPLUMA / IPILRA compliance) |

### PTO Status Lifecycle

```
 PENDING ──[APPROVE]──▶ ACTIVE ──[SUSPEND]──▶ SUSPENDED
    ▲                      │                      │
    │                      │                      │
    │               [REVOKE]│               [REACTIVATE]
    │                      │                      │
    └────[REINSTATE]───────┘                      │
                                                    │
                                              [REVOKE]──▶ REVOKED
```

- **PENDING**: Created by Land Officer / Data Capturer; awaiting TA Administrator approval.
- **ACTIVE**: Occupancy rights conferred; parcel status set to `ALLOCATED`.
- **SUSPENDED**: Temporarily paused; occupancy rights suspended.
- **REVOKED**: Permanently withdrawn; parcel returned to `AVAILABLE`.
- **EXPIRED**: Term reached; no longer valid.
- **REINSTATED**: REVOKED status overturned by TA Administrator; restores ACTIVE.

### Role-Based Access Control

| Role | PTO Permissions |
|---|---|
| `ROLE_LAND_OFFICER` | Create parcels, create PTOs, upload documents |
| `ROLE_DATA_CAPTURER` | Create PTOs, upload documents, edit PENDING PTOs |
| `ROLE_TA_ADMINISTRATOR` | Approve, suspend, reactivate, revoke, reinstate PTOs |
| `ROLE_SYSTEM_ADMIN` | Full access to all PTO operations |
| `ROLE_REPORT_VIEWER` | Read-only search and report access |

### Database Schema Changes

Migration `V24__extend_pto_with_allocation_and_survey_fields.sql` adds:

| Column | Type | Purpose |
|---|---|---|
| `allocated_by` | VARCHAR(150) | Headman / Chief who allocated the stand |
| `allocation_date` | DATE | Date of TA allocation (distinct from issue date) |
| `stand_area` | DOUBLE PRECISION | Stand extent in m² |
| `survey_reference` | VARCHAR(100) | Surveyor General diagram / general plan reference |
| `boundary_description` | TEXT | Beacons, fences, natural features |
| `allocation_fee_receipt` | VARCHAR(100) | Proof of TA allocation fee payment |
| `ta_recommendation_ref` | VARCHAR(100) | TA letter / recommendation reference |
| `community_resolution_required` | BOOLEAN | Set TRUE for AGRICULTURAL / BUSINESS PTOs |

### API Endpoints

#### PTO REST API (JSON)

```http
POST /api/ptos
GET /api/ptos
GET /api/ptos/{id}
GET /api/ptos/number/{ptoNumber}
PATCH /api/ptos/{id}/approve
PATCH /api/ptos/{id}/suspend
PATCH /api/ptos/{id}/reactivate
PATCH /api/ptos/{id}/revoke
PATCH /api/ptos/{id}/reinstate
```

#### Document Upload (Multipart)

```http
POST /api/documents/upload
Content-Type: multipart/form-data

file: <binary>
request: {"documentType":"TA_ALLOCATION_LETTER","entityType":"PTO","entityId":123}
```

### Legal Compliance Checklist

- [x] PTO records linked to Traditional Authority and Village
- [x] ID validation via Luhn algorithm (13-digit SA ID)
- [x] TA Allocation Letter required before approval
- [x] Site Sketch required before approval
- [x] Community Resolution tracking for commercial/agricultural allocations
- [x] Immutable audit trail for all status changes (created_by, approved_by, etc.)
- [x] Document upload logged with checksum (SHA-256) and access logs
- [x] Duplicate PTO prevention per parcel and per ID number
- [x] Parcel-stand number consistency validation
- [x] POWI (Proof of Work Information) for all land allocations

### Additional Digital Transformation Features

#### 1. Spatial Validation with PostGIS

PostGIS is enabled on the `parcels` table. A database trigger (`V26`) auto-updates the `geometry` column from boundary points. On parcel creation or update, `ParcelServiceImpl` checks for overlapping parcels using `ST_Intersects` and rejects the operation if a boundary dispute is detected.

- **Migration**: `V26__create_parcel_geometry_trigger.sql`
- **Validation**: `ParcelServiceImpl.checkForOverlaps()`
- **Benefit**: Prevents boundary disputes at the time of land allocation

#### 2. E-Signature for PTO Approvals

PTO approvals now support digital signatures. The `PTOApprovalRequest` DTO includes `signatureData`, `signatureImagePath`, `ipAddress`, and `userAgent`. When a PTO is approved, the signature is persisted to `pto_approval_signatures` for audit and legal admissibility.

- **Entity**: `pto_approval_signatures` table (Migration `V25`)
- **Workflow**: Signature captured during approval → persisted with metadata
- **Benefit**: Legally binding digital approval trail for TA administrators

#### 3. Automated PTO Expiry

A scheduled job (`@Scheduled`) runs daily at 01:00 to scan for ACTIVE PTOs whose `expiryDate` has passed and transitions them to `EXPIRED`. The job publishes `PTOExpiredEvent` for downstream notifications.

- **Scheduler**: `PTOExpiryScheduler`
- **Cron**: `0 0 1 * * *` (daily at 01:00)
- **Benefit**: Eliminates manual status tracking; ensures expired PTOs are flagged automatically

#### 4. Notification Service

A new `notification` module provides email, SMS, and in-app notification capabilities. Event listeners (`PTONotificationListener`) subscribe to PTO lifecycle events and dispatch notifications automatically.

- **Module**: `za.co.taloms.notification`
- **Channels**: `EMAIL`, `SMS`, `IN_APP`
- **Events**: `PTOCreatedEvent`, `PTOApprovedEvent`, `PTORevokedEvent`, `PTOSuspendedEvent`, `PTOReinstatedEvent`, `PTOExpiredEvent`
- **Benefit**: Real-time stakeholder communication; audit-ready notification log

#### 5. PDF Certificate Generation

Approved PTOs can be downloaded as official PDF certificates via `GET /api/ptos/{id}/certificate`. The `PTOCertificatePdfGenerator` uses Apache PDFBox to produce a formatted A4 certificate containing all PTO details, allocation metadata, and issue/expiry dates.

- **Generator**: `PTOCertificatePdfGenerator`
- **Endpoint**: `GET /api/ptos/{id}/certificate`
- **Benefit**: Instant printable proof of occupancy rights for holders and authorities

---

## License

**Private — All Rights Reserved.**

This software is intended for use by Traditional Authority offices in South Africa.
Unauthorised distribution or modification is prohibited.

---

<div align="center">

**TALOMS v1.0.0 — Phase 1 Internal Release**

*Traditional Authority Land & Occupancy Management System*

Built with Java 21 · Spring Boot · PostgreSQL + PostGIS

</div>
