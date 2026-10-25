# Kettle & Kitchen — Cafe Management System

A complete, working **internal** cafe management system. Customers never log in: they walk into
the cafe and a staff member records the order. The system covers the full counter flow —
menu setup, order capture, billing with a PDF invoice, and revenue reporting.

Built as a classical three-tier application:

| Layer     | Technology                                                                                 |
| --------- | ------------------------------------------------------------------------------------------ |
| Frontend  | Angular 22 (standalone components, signals), TypeScript, SCSS, Angular Material 3           |
| Backend   | Java 21, Spring Boot 4, Spring Web, Spring Data JPA / Hibernate, Spring Security, JWT, Maven |
| Database  | MySQL 8                                                                                     |
| Extras    | Bean Validation, BCrypt, OpenPDF (invoice), Vitest + JUnit 5                                 |

> There is **no** React, Node.js, Express, MongoDB or Firebase anywhere in this project.

---

## Table of contents

1. [What the system does](#1-what-the-system-does)
2. [Repository layout](#2-repository-layout)
3. [Prerequisites](#3-prerequisites)
4. [Database setup](#4-database-setup)
5. [Configuration and secrets](#5-configuration-and-secrets)
6. [Running the backend](#6-running-the-backend)
7. [Running the frontend](#7-running-the-frontend)
8. [Default accounts](#8-default-accounts)
9. [End-to-end walkthrough](#9-end-to-end-walkthrough)
10. [Running the tests](#10-running-the-tests)
11. [API overview](#11-api-overview)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. What the system does

**Two roles only — `ADMIN` and `STAFF`.** Authorisation is enforced by Spring Security on the
server, not just hidden in the UI.

| Feature                    | ADMIN | STAFF | Notes                                                                       |
| -------------------------- | :---: | :---: | --------------------------------------------------------------------------- |
| Login / logout             |   ✅   |   ✅   | JWT (HS256), BCrypt password hashes                                          |
| Change own password        |   ✅   |   ✅   | Verifies the current password first                                          |
| Dashboard & revenue        |   ✅   |   ❌   | `/api/dashboard/**` is ADMIN-only                                            |
| Category management        |   ✅   |  view | Staff can read categories to browse products                                 |
| Product management         |   ✅   |  view | Staff never sees create/edit/delete controls                                 |
| Create order               |   ✅   |   ✅   | Both roles take orders at the counter                                        |
| Cancel order               |   ✅   |   ❌   | `@PreAuthorize("hasRole('ADMIN')")`                                          |
| Generate bill, update payment status |   ✅   |   ✅   | Pending → Paid, no real payment gateway                                      |
| Cancel bill                |   ✅   |   ❌   | Admin only                                                                   |
| PDF invoice download/print |   ✅   |   ✅   | Generated server-side with OpenPDF                                           |
| Staff account management   |   ✅   |   ❌   | `/api/users/**` is ADMIN-only — a staff member cannot grant themselves admin |

**Business rules that are enforced on the server:**

- Order totals are **always recalculated from the prices stored in MySQL**; the browser's total is
  a preview only.
- Each `order_items` row stores a **price snapshot** (`unit_price`, `product_name_snapshot`) so old
  invoices stay correct even if the menu price changes later.
- Order creation runs inside a single `@Transactional` method, so a half-saved order can never exist.
- All money uses `BigDecimal`, never `double`.
- Exactly **one bill per order** — enforced by a database unique constraint and checked in the service.
- Revenue **excludes** `PENDING` and `CANCELLED` bills (only `PAID` bills count).
- Passwords are never returned by any API and are never logged.

---

## 2. Repository layout

```
cafeManagement/
├── README.md                     ← you are here
├── docs/
│   ├── DATABASE-SCHEMA.md        ← tables, columns, keys, relationships
│   ├── API-DOCUMENTATION.md      ← every endpoint with request/response examples
│   ├── INTERVIEW-PREPARATION.md  ← Java / Spring / Angular revision notes
│   └── postman/
│       └── Cafe-Management-System.postman_collection.json
├── backend/                      ← Spring Boot application
│   ├── .env.example              ← template for the environment variables
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/
│       ├── main/java/com/cafe/management/
│       │   ├── CafeManagementApplication.java
│       │   ├── config/           ← SecurityConfig, DataInitializer
│       │   ├── controller/       ← REST controllers
│       │   ├── dto/              ← Java records (request/response shapes)
│       │   ├── entity/           ← JPA entities + enums
│       │   ├── exception/        ← custom exceptions + @RestControllerAdvice
│       │   ├── repository/       ← Spring Data JPA repositories
│       │   ├── security/         ← JWT filter, JwtService, UserDetailsService
│       │   ├── service/ + service/impl/
│       │   └── util/
│       ├── main/resources/application.properties
│       └── test/java/...         ← JUnit 5 tests
└── frontend/                     ← Angular application
    ├── angular.json
    ├── package.json
    └── src/
        ├── environments/         ← apiBaseUrl + cafe details
        ├── index.html
        ├── main.ts
        ├── styles.scss           ← global theme + design tokens
        └── app/
            ├── app.config.ts, app.routes.ts, app.ts
            ├── core/             ← models, services, guards, interceptors, utils
            ├── shared/           ← layout shell, dialogs, helpers
            └── features/         ← auth, dashboard, categories, products,
                                    orders, billing, users, profile
```

---

## 3. Prerequisites

| Tool           | Version used here | Check with            |
| -------------- | ----------------- | --------------------- |
| JDK            | 21 (or newer)     | `java -version`       |
| Maven          | via `mvnw.cmd`    | `.\mvnw.cmd -v`       |
| Node.js        | 20+               | `node -v`             |
| npm            | 10+               | `npm -v`              |
| MySQL Server   | 8.x               | `mysql --version`     |
| MySQL Workbench| any               | (optional, GUI)       |

Roughly **2 GB** of free disk space is needed for Maven and npm dependencies.

---

## 4. Database setup

The backend connects to:

```
jdbc:mysql://localhost:3306/cafe_management_db
```

You have two options.

### Option A — let Spring Boot create the database (easiest)

The JDBC URL already contains `createDatabaseIfNotExist=true`, so simply having MySQL running and
the credentials correct is enough. Hibernate creates all tables on first start.

### Option B — create it yourself in MySQL Workbench

```sql
CREATE DATABASE IF NOT EXISTS cafe_management_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Then connect to `cafe_management_db` and run `SELECT * FROM users;` after the first backend start to
confirm Hibernate created the schema.

### Schema management strategy

`spring.jpa.hibernate.ddl-auto` defaults to **`update`**:

- On every start Hibernate compares the entities with the live schema and adds anything missing
  (new tables, new columns).
- It **never drops or deletes** existing data, which is safe for a running cafe.
- For production you would switch this to `validate` and manage changes with Flyway or Liquibase —
  this project deliberately keeps `update` so a fresh clone works with a single command.

Full table/column documentation: [docs/DATABASE-SCHEMA.md](docs/DATABASE-SCHEMA.md).

---

## 5. Configuration and secrets

**No credential is stored in a tracked file.** `application.properties` only references environment
variables and provides development-safe defaults:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/cafe_management_db?...}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
app.jwt.secret=${JWT_SECRET:cafe-management-system-development-secret-key-change-me}
app.admin.email=${ADMIN_EMAIL:admin@cafe.com}
app.admin.password=${ADMIN_PASSWORD:Admin@123}
```

`backend/.env.example` documents every supported variable. Copy it to `backend/.env` for your own
reference — the `.gitignore` excludes `.env`, `*.env` (except `.env.example`) and
`application-local.properties`.

### Setting the variables on Windows

**PowerShell — current session only** (recommended while developing):

```powershell
$env:DB_PASSWORD  = "your_mysql_root_password"
$env:JWT_SECRET   = "a-long-random-secret-of-at-least-32-characters"
```

**PowerShell — permanently for your user account** (open a new terminal afterwards):

```powershell
setx DB_PASSWORD "your_mysql_root_password"
setx JWT_SECRET  "a-long-random-secret-of-at-least-32-characters"
```

**Command Prompt (cmd.exe):**

```cmd
set DB_PASSWORD=your_mysql_root_password
```

> ⚠️ If your MySQL `root` user has no password, simply do not set `DB_PASSWORD` — the default is
> already empty.
>
> ⚠️ Never paste the real password into `application.properties`, `.env.example`, the README, a
> commit message or a screenshot.

---

## 6. Running the backend

```powershell
cd backend

# 1. (first run only) point the app at your MySQL instance
$env:DB_PASSWORD = "your_mysql_root_password"

# 2. start the API on http://localhost:8080
.\mvnw.cmd spring-boot:run
```

You should see `Started CafeManagementApplication` and `Tomcat started on port 8080`.

On the very first start `DataInitializer` seeds:

- the bootstrap **ADMIN** account (from `ADMIN_EMAIL` / `ADMIN_PASSWORD`),
- one demo **STAFF** account (only if no staff user exists yet),
- five sample categories and eleven sample products (only if the tables are empty).

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/auth/login `
  -Method Post -ContentType 'application/json' `
  -Body (@{ email = 'admin@cafe.com'; password = 'Admin@123' } | ConvertTo-Json)
```

To build a runnable jar instead:

```powershell
cd backend
.\mvnw.cmd clean package
java -jar target\cafe-management-0.0.1-SNAPSHOT.jar
```

---

## 7. Running the frontend

```powershell
cd frontend
npm install --legacy-peer-deps      # first run only
npm start                            # http://localhost:4200
```

The dev server proxies nothing — the app calls `http://localhost:8080/api` directly
(`src/environments/environment.ts`), and the backend enables CORS for `http://localhost:*`.

Production build:

```powershell
cd frontend
npm run build                        # output in dist/frontend/browser
```

In production the bundle calls `/api` on its own origin (`environment.production.ts`), so serve the
static files behind a reverse proxy that forwards `/api/**` to the Spring Boot service.

**Start order:** MySQL → backend → frontend.

---

## 8. Default accounts

Created automatically on first start (change these values before any real deployment):

| Role  | Email            | Password    |
| ----- | ---------------- | ----------- |
| ADMIN | `admin@cafe.com` | `Admin@123` |
| STAFF | `ravi@cafe.com`  | `Staff@123` |

The login screen also has one-click buttons that fill these demo credentials.

**Important:** the admin password is only used to create the account when that email does not exist
yet. Change it from **My profile → Change password** (or by setting `ADMIN_PASSWORD` before the very
first start).

---

## 9. End-to-end walkthrough

1. Sign in as the admin → you land on **Dashboard**.
2. **Categories → New category** — e.g. `Coffee`, `Tea`, `Snacks`, `Desserts`, `Cold Beverages`.
3. **Products → New product** — set name, price, category and availability.
4. **Staff → New account** — create a login for a staff member.
5. Sign out and sign in as that staff member → you land on **New Order**, and the Dashboard / Staff
   menu entries are gone.
6. **New Order** — pick products, adjust quantities, remove lines, optionally enter the customer's
   name and phone. The grand total updates live.
7. **Save order** — the backend re-validates availability, recalculates every line from the database
   and saves the order plus its items in one transaction.
8. On the order page choose **Generate bill**, pick Cash / UPI / Card and Paid / Pending.
9. On the invoice page: **Download PDF**, **Print invoice**, switch the payment status, or (admin)
   **Cancel bill**.
10. Sign back in as the admin — **Dashboard** revenue and bill counters now include the new sale.
    `PENDING` and `CANCELLED` bills are excluded from revenue.

---

## 10. Running the tests

**Backend (JUnit 5 + Mockito):**

```powershell
cd backend
.\mvnw.cmd test
```

Covers order total calculation from database prices, the "product unavailable" rejection,
duplicate-bill prevention, bill cancellation, JWT issuing/parsing and the user-details lookup.

**Frontend (Vitest):**

```powershell
cd frontend
npm test
```

Covers HTTP error message extraction and the authentication state (login, role resolution, logout).

---

## 11. API overview

Full request/response documentation with examples: [docs/API-DOCUMENTATION.md](docs/API-DOCUMENTATION.md).
A ready-to-import Postman collection is in [docs/postman/](docs/postman/).

| Method | Endpoint                             | Access      | Purpose                          |
| ------ | ------------------------------------ | ----------- | -------------------------------- |
| POST   | `/api/auth/login`                    | public      | Sign in, returns a JWT           |
| GET    | `/api/auth/me`                       | authenticated | Current user profile           |
| POST   | `/api/auth/change-password`          | authenticated | Change own password            |
| GET/POST/PUT/DELETE | `/api/categories[/{id}]`  | read: any · write: ADMIN | Category CRUD   |
| GET    | `/api/products?categoryId=&search=`  | authenticated | Search / filter products        |
| POST/PUT/DELETE | `/api/products[/{id}]`      | ADMIN       | Product CRUD                     |
| PATCH  | `/api/products/{id}/availability`    | ADMIN       | Toggle availability              |
| POST   | `/api/orders`                        | ADMIN+STAFF | Create an order                  |
| GET    | `/api/orders?search=&status=`        | ADMIN+STAFF | List / search orders             |
| GET    | `/api/orders/recent?limit=5`         | ADMIN+STAFF | Latest orders                    |
| POST   | `/api/orders/{id}/cancel`            | ADMIN       | Cancel an order                  |
| POST   | `/api/bills`                         | ADMIN+STAFF | Generate a bill                  |
| GET    | `/api/bills?search=&status=`         | ADMIN+STAFF | Bill history                     |
| PATCH  | `/api/bills/{id}/payment-status`     | ADMIN+STAFF | Update payment status            |
| POST   | `/api/bills/{id}/cancel`             | ADMIN       | Cancel a bill                    |
| GET    | `/api/bills/{id}/pdf`                | ADMIN+STAFF | Download the PDF invoice         |
| GET    | `/api/users?search=`                 | ADMIN       | List staff accounts              |
| POST   | `/api/users`                         | ADMIN       | Create a staff/admin account     |
| PATCH  | `/api/users/{id}/status`             | ADMIN       | Enable / disable an account      |
| GET    | `/api/dashboard/stats`               | ADMIN       | Dashboard aggregates             |

All authenticated requests need the header:

```
Authorization: Bearer <token>
```

---

## 12. Troubleshooting

| Symptom                                                    | Cause / fix                                                                                                                    |
| ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `Cannot reach the server` on the login page                 | The backend is not running, or it crashed. Check the terminal for `Started CafeManagementApplication`.                          |
| `Access denied` / MySQL error on startup                    | `DB_PASSWORD` is wrong or MySQL is not running. Verify with `Get-Service MySQL80`.                                              |
| `Communications link failure`                               | MySQL is listening on another port. Set `DB_URL` accordingly, e.g. `jdbc:mysql://localhost:3307/...`.                           |
| Login always fails                                          | The seeded password was changed, or the account is `INACTIVE`. An admin can re-enable it under **Staff**.                       |
| `Port 8080 was already in use`                              | Start on another port: `$env:SERVER_PORT = "8081"` (then update `environment.ts`).                                              |
| `Port 4200 is already in use`                               | `npm start -- --port 4300`.                                                                                                     |
| CORS error in the browser console                           | The frontend runs on an origin other than `http://localhost:*` / `http://127.0.0.1:*`. Extend `allowedOriginPatterns`. On Render set `CORS_ALLOWED_ORIGINS` to the frontend URL (e.g. `https://your-frontend.onrender.com`). |
| Opening the backend URL shows `401 Unauthorized`            | That was `GET /` being treated as a protected API route. It now returns `{ "status": "UP" }`. Set Render **Health Check Path** to `/health`. |
| First page load takes several seconds on Render             | Free Render services sleep after idle time (cold start). Also keep `CORS_ALLOWED_ORIGINS` set, and point Render's health check at `/health` so the JVM stays up. |
| 401 immediately after a while                               | The JWT expired (`JWT_EXPIRATION_MS`, default 24 h). The interceptor signs you out and sends you to the login page.             |
| `npm install` crashes with `edgesOut`                       | Known npm 10 arborist bug — use `npm install --legacy-peer-deps`.                                                                |
| A product cannot be deleted                                 | It is referenced by `order_items`. This is intentional: historical bills must stay intact. Disable it instead.                  |

---

## Security notes

- Passwords are hashed with **BCrypt** and never leave the server.
- Sessions are **stateless**; the JWT is the only credential. CSRF protection is therefore disabled
  (no cookie-based sessions exist).
- The JWT secret must be at least 32 characters for HS256 and must be overridden outside of local
  development.
- Role checks exist in **three** independent places: the route guards (UX), `@PreAuthorize` on the
  controller methods (business rules) and the URL rules in `SecurityConfig` (coarse gate).
- `@PreAuthorize("hasRole('ADMIN')")` is what actually prevents a staff member from granting
  themselves admin rights — hiding buttons in Angular is only cosmetic.
