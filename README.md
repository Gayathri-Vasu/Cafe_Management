# Kettle & Kitchen — Cafe Management System

A full-stack cafe management system for handling menu management, orders, billing, PDF invoices, and revenue tracking. Designed for internal use by cafe administrators and staff.

## Tech Stack

| Layer | Technologies |
|---|---|
| Frontend | Angular 22, TypeScript, SCSS, Angular Material |
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA |
| Database | MySQL 8 |
| Authentication | JWT, BCrypt |
| Testing | JUnit 5, Mockito, Vitest |
| PDF Generation | OpenPDF |

## Features

- **Authentication:** Secure login with ADMIN and STAFF roles.
- **Dashboard:** Revenue reports and sales statistics for administrators.
- **Category Management:** Organize cafe menu categories.
- **Product Management:** Manage products, prices, and availability.
- **Order Management:** Create and track customer orders.
- **Billing:** Generate bills and update payment status.
- **PDF Invoices:** Download and print invoices.
- **Staff Management:** Create and manage staff accounts.
- **Profile Management:** Change passwords securely.

## Role-Based Access

| Feature | ADMIN | STAFF |
|---|---|---|
| Dashboard and revenue | Yes | No |
| View categories and products | Yes | Yes |
| Manage categories and products | Yes | No |
| Create orders | Yes | Yes |
| Generate bills and update payment status | Yes | Yes |
| Cancel orders and bills | Yes | No |
| Manage staff accounts | Yes | No |

Permissions are enforced by the backend using Spring Security, not just by hiding frontend controls.

## Project Structure

```text
cafeManagement/
├── backend/
│   ├── src/main/java/com/cafe/management/
│   ├── src/main/resources/
│   ├── src/test/
│   ├── pom.xml
│   └── .env.example
├── frontend/
│   ├── src/app/
│   ├── src/environments/
│   ├── angular.json
│   └── package.json
├── docs/
│   ├── DATABASE-SCHEMA.md
│   ├── API-DOCUMENTATION.md
│   ├── INTERVIEW-PREPARATION.md
│   └── postman/
└── README.md
```

## Prerequisites

Install the following before running the application:

- JDK 21 or newer
- Node.js 20 or newer
- npm 10 or newer
- MySQL 8

## Setup and Run

### 1. Database

Start MySQL and create the database:

```sql
CREATE DATABASE cafe_management_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

Configure your database credentials using environment variables.

### 2. Backend

Open a terminal in the project directory:

```powershell
cd backend
$env:DB_PASSWORD = "your_mysql_password"
.\mvnw.cmd spring-boot:run
```

The backend starts at `http://localhost:8080`.

Configure the required database and JWT environment variables before running the application. Refer to `backend/.env.example`.

### 3. Frontend

Open another terminal:

```powershell
cd frontend
npm install --legacy-peer-deps
npm start
```

The frontend starts at `http://localhost:4200`.

## Default Accounts

The application can initialize default accounts on first startup.

| Role | Email | Default Password |
|---|---|---|
| ADMIN | `admin@cafe.com` | `Admin@123` |
| STAFF | `ravi@cafe.com` | `Staff@123` |

These credentials apply only if the corresponding accounts were created with the documented defaults. Change default passwords before any real deployment.

## Running Tests

**Backend:**

```powershell
cd backend
.\mvnw.cmd test
```

**Frontend:**

```powershell
cd frontend
npm test
```

## API

The backend exposes REST APIs for authentication, categories, products, orders, bills, users, and dashboard statistics.

See [API Documentation](docs/API-DOCUMENTATION.md) for endpoints and request/response examples.

## Security and Business Rules

- Passwords are hashed using BCrypt.
- Protected API endpoints enforce role-based access.
- Order totals are recalculated by the backend using database prices.
- Order items preserve price snapshots for historical invoices.
- Order creation uses transactions.
- Monetary calculations use `BigDecimal`.
- Revenue calculations exclude pending and cancelled bills.
- Database credentials and production secrets must be supplied through environment variables and must not be committed to Git.

## Deployment

The application can be deployed with the Angular frontend, Spring Boot backend, and MySQL database hosted separately.

Configure production environment variables, CORS origins, database connectivity, and the frontend API URL for the deployment environment.

---

**Kettle & Kitchen — Cafe Management System**
