# Architecture

## Overview
Society Management is a monolithic Spring Boot REST API with a modular package structure, designed to be split into microservices in the future.

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT |
| Database | MongoDB Atlas |
| Documentation | Springdoc OpenAPI |
| Containerization | Docker |
| Orchestration | Kubernetes |
| CI/CD | GitHub Actions |
| Observability | Prometheus + Grafana |
| Testing | JUnit 5 + Mockito |

## Module Structure

com.Application.SocietyManagement/
├── communication/     Announcements & alerts
├── core/
│   ├── common/       BaseEntity
│   ├── config/       Security, MongoDB, OpenAPI, AdminSeeder
│   ├── exception/    Global exception handling
│   ├── logging/      Request logging filter
│   └── security/     JWT filter
├── finance/          Maintenance billing
├── issue/            Issue tracking & voting
└── users/            Auth, JWT, user management

## Security Flow
Request → RequestLoggingFilter → JwtAuthenticationFilter
→ SecurityFilterChain → Controller → Service → Repository

## Data Flow
Client → REST Controller → Service → Repository → MongoDB Atlas

## Key Design Decisions
- **MongoDB** — flexible schema for society data that varies by configuration
- **JWT stateless auth** — no session storage, scales horizontally
- **Role-based access** — ADMIN and RESIDENT roles enforced at method level
- **Approval workflow** — new residents start as PENDING, admin approves
- **Modular packages** — each module is self-contained (controller, dto, entity, repository, service)