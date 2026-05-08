# 🏘️ Society Management System

A production-ready REST API for managing residential societies — built with **Spring Boot 3.5**, **MongoDB**, **Docker**, and **Kubernetes**.

[![CI/CD](https://github.com/RajsProjects/society-management/actions/workflows/ci.yml/badge.svg)](https://github.com/RajsProjects/society-management/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-green.svg)](https://www.mongodb.com/cloud/atlas)
[![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [API Overview](#-api-overview)
- [Running Tests](#-running-tests)
- [Docker Setup](#-docker-setup)
- [Observability](#-observability)
- [Contributing](#-contributing)
- [Good First Issues](#-good-first-issues)
- [Roadmap](#-roadmap)

---

## ✨ Features

| Module | Description |
|--------|-------------|
| 🔐 **Auth** | JWT authentication with role-based access (ADMIN, RESIDENT) |
| 👥 **User Management** | Resident approval workflow — PENDING → ACTIVE |
| 🚨 **Issue Tracking** | Report problems, vote on priority, track resolution |
| 💰 **Maintenance Billing** | Generate bills, simulate UPI payments, auto-mark overdue |
| 📢 **Announcements** | Broadcast categorized alerts (GENERAL, MAINTENANCE, EMERGENCY) |
| 📊 **Observability** | Prometheus metrics + Grafana dashboards |
| 📖 **API Docs** | Swagger UI with JWT auth built in |

---

## 🏗️ Architecture

┌─────────────────────────────────────────────────────────┐
│                     Client (Postman / Frontend)         │
└─────────────────────────┬───────────────────────────────┘
                          │ HTTP
┌─────────────────────────▼───────────────────────────────┐
│                    Spring Boot API                      │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐ │
│  │RequestLogging│  │JwtAuth       │  │GlobalException │ │
│  │Filter        │  │Filter        │  │Handler         │ │
│  └──────────────┘  └──────────────┘  └────────────────┘ │
│                                                         │
│  ┌──────────┐ ┌────────┐ ┌─────────┐ ┌──────────────┐   │
│  │   Auth   │ │ Users  │ │ Issues  │ │   Finance    │   │
│  │ Module   │ │ Module │ │ Module  │ │   Module     │   │
│  └──────────┘ └────────┘ └─────────┘ └──────────────┘   │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │            Communication Module                  │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                   MongoDB Atlas                         │
│  users │ issues │ issue_votes │ maintenance_bills │     │
│  announcements                                          │
└─────────────────────────────────────────────────────────┘

### Request Flow
Request
→ RequestLoggingFilter  (logs method, path, duration, traceId)
→ JwtAuthenticationFilter  (validates JWT, sets SecurityContext)
→ SecurityFilterChain  (checks roles via @PreAuthorize)
→ Controller  (@RestController)
→ Service  (business logic)
→ Repository  (MongoDB)
→ Response

### User Approval Flow
POST /auth/signup
│
▼
Status: PENDING ──── Login attempt ──→ 403 Forbidden
│
│  Admin calls PATCH /users/{id}/status
▼
Status: ACTIVE ───── Login attempt ──→ 200 + JWT Token
│
│  Admin calls PATCH /users/{id}/status
▼
Status: INACTIVE ─── Login attempt ──→ 403 Forbidden

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JJWT 0.12.6 |
| Database | MongoDB Atlas |
| Documentation | Springdoc OpenAPI + Swagger UI |
| Containerization | Docker |
| Orchestration | Kubernetes |
| CI/CD | GitHub Actions |
| Observability | Prometheus + Grafana |
| Testing | JUnit 5 + Mockito |
| Build | Maven 3.9 |

---

## 🚀 Quick Start

### Prerequisites
- Java 21
- Maven 3.9+
- MongoDB Atlas account ([free tier](https://www.mongodb.com/cloud/atlas/register))
- Docker Desktop (optional)

### 1. Clone the repository

```bash
git clone https://github.com/RajsProjects/society-management.git
cd society-management
```

### 2. Configure the application

```bash
cp src/main/resources/application-template.yml src/main/resources/application.yml
```

Edit `application.yml`:
```yaml
spring:
  data:
    mongodb:
      uri: mongodb+srv://USERNAME:PASSWORD@cluster.mongodb.net/Society_DB

jwt:
  secret: YOUR_SECRET_MIN_32_CHARS
```

### 3. Run the application

```bash
mvn spring-boot:run
```

### 4. Access Swagger UI

http://localhost:8080/swagger-ui.html

### 5. Login with default admin

```json
POST /api/v1/auth/login
{
  "email": "admin@society.com",
  "password": "admin123"
}
```

> ⚠️ Change the default admin password immediately in production.

---

## 📡 API Overview

### Authentication
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/signup` | Public | Register resident (starts PENDING) |
| POST | `/api/v1/auth/login` | Public | Login and get JWT token |

### User Management
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/users` | ADMIN | List users with filters |
| PATCH | `/api/v1/users/{id}/status` | ADMIN | Approve or block resident |

### Issues
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/issues` | ALL | Report a new issue |
| GET | `/api/v1/issues` | ALL | List all issues with vote counts |
| PATCH | `/api/v1/issues/{id}/status` | ADMIN | Update issue status |
| PATCH | `/api/v1/issues/{id}/priority` | ADMIN | Set issue priority |
| POST | `/api/v1/issues/{id}/votes` | ALL | Vote on an issue |
| DELETE | `/api/v1/issues/{id}/votes` | ALL | Remove your vote |

### Finance
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/finance/bills` | ADMIN | Generate maintenance bill |
| GET | `/api/v1/finance/bills` | ALL | View bills (residents see own only) |
| POST | `/api/v1/finance/bills/{id}/pay` | RESIDENT | Simulate UPI payment |

### Announcements
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/announcements` | ADMIN | Create announcement |
| GET | `/api/v1/announcements` | ALL | List announcements |
| GET | `/api/v1/announcements/{id}` | ALL | Get single announcement |
| PUT | `/api/v1/announcements/{id}` | ADMIN | Update announcement |
| DELETE | `/api/v1/announcements/{id}` | ADMIN | Delete announcement |

> Full interactive documentation at `/swagger-ui.html`

### Error Response Format
```json
{
  "timestamp": "2026-05-07T10:00:00Z",
  "status": 404,
  "error": "User not found",
  "path": "/api/v1/users/abc123",
  "traceId": "a3f2b1c4"
}
```

---

## 🧪 Running Tests

```bash
mvn test
```

78 unit tests across all service layers — no database connection required.

| Test Class | Coverage |
|-----------|---------|
| `AuthServiceTest` | Signup, login, password hashing, status blocking |
| `UserServiceTest` | Pagination, filtering, status updates, admin protection |
| `JwtServiceTest` | Token generation, extraction, expiry, tampering |
| `IssueServiceTest` | Create, vote integrity, status/priority updates |
| `MaintenanceBillServiceTest` | Billing, payment rules, overdue scheduler |
| `AnnouncementServiceTest` | CRUD, type filtering, pagination |

---

## 🐳 Docker Setup

### Run with Docker

```bash
# Copy and configure environment
cp .env.example .env
# Edit .env with your MongoDB URI and JWT secret

# Build and run
docker build -t society-management:latest .
docker run -p 8080:8080 --env-file .env society-management:latest
```

### Run with Docker Compose (includes Prometheus + Grafana)

```bash
docker-compose up -d
```

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

Grafana default login: `admin / admin123`

---

## 📊 Observability

The app exposes metrics at `/actuator/prometheus`. Grafana dashboards track:

- HTTP request rate and response times (p99)
- JVM memory usage
- Active HTTP connections
- MongoDB operation counts
- Thread count

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a pull request.

```bash
# Fork the repo, then:
git clone https://github.com/YOUR_USERNAME/society-management.git
git checkout -b feat/your-feature-name
# make changes
mvn test  # make sure all tests pass
git commit -m "feat: your feature description"
git push origin feat/your-feature-name
# open a Pull Request
```

---

## 🌱 Good First Issues

New to the project? Start here:

| Issue | Difficulty | Skills |
|-------|-----------|--------|
| [Add validation to SignupRequest](https://github.com/RajsProjects/society-management/issues) | ⭐ Easy | Java, Bean Validation |
| [Improve health check response](https://github.com/RajsProjects/society-management/issues) | ⭐ Easy | Java, Spring Boot |
| [Add hasNext/hasPrevious to PagedResponse](https://github.com/RajsProjects/society-management/issues) | ⭐ Easy | Java, Spring Data |
| [Add createdAt/updatedAt to all DTOs](https://github.com/RajsProjects/society-management/issues) | ⭐ Easy | Java, Lombok |
| [Write missing unit tests](https://github.com/RajsProjects/society-management/issues) | ⭐⭐ Medium | JUnit 5, Mockito |

---

## 🗺️ Roadmap

- [x] Auth module with JWT
- [x] User management and approval workflow
- [x] Issue tracking with voting
- [x] Maintenance billing with payment simulation
- [x] Announcements module
- [x] Global exception handling
- [x] Swagger UI documentation
- [x] Docker + Kubernetes support
- [x] GitHub Actions CI/CD
- [x] Prometheus + Grafana observability
- [ ] Email notifications on bill due
- [ ] Visitor management module
- [ ] Multi-society support (tenant isolation)
- [ ] Payment gateway integration (Razorpay)
- [ ] Resident self-registration with OTP
- [ ] Rate limiting per IP
- [ ] Frontend (React / Angular)
- [ ] Mobile app (Flutter)

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

- Open an [issue](https://github.com/RajsProjects/society-management/issues)
- Read the [setup guide](docs/SETUP.md)
- Check the [architecture docs](docs/ARCHITECTURE.md)
- Browse the [API reference](docs/API.md)
