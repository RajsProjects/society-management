# Folder and Code Architecture

## Repository map

```text
SocietyManagement/
├── src/main/java/com/Application/SocietyManagement/
│   ├── communication/       announcements, email events, templates integration
│   ├── complaint/           complaint lifecycle and resident reports
│   ├── dashboard/           dashboard/summary read models and endpoints
│   ├── finance/             maintenance bills and payment workflows
│   ├── flat/                flat/unit and occupancy data
│   ├── issue/               issue lifecycle, status, priority, and voting
│   ├── society/             society registration, joining, and tenant context
│   ├── subscription/        plans, entitlements, and enforcement
│   ├── users/               signup, login, JWT, user management
│   └── core/
│       ├── common/          shared entities and cross-module primitives
│       ├── config/          Spring, MongoDB, Redis, AWS, payment, and OpenAPI setup
│       ├── exception/       global exception mapping and error contracts
│       ├── logging/         request/structured logging
│       └── security/        authentication and request security filters
├── src/main/resources/
│   ├── application*.yml     environment configuration
│   ├── templates/email/     Thymeleaf email templates
│   └── logback-spring.xml   logging configuration
├── src/test/                 unit and integration tests
├── k8s/                      Kubernetes namespace, deployment, service, HPA, config, secrets
├── observability/            Prometheus and Grafana provisioning
├── k6/                       load-test scenarios
├── docs/                     API, setup, architecture, and interview notes
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

Some package names may differ as the code evolves. Treat the actual directory tree as authoritative and update this map when a module is added or renamed.

## Feature-module convention

Most business modules follow this dependency direction:

```text
controller -> service -> repository -> MongoDB
     |            |
    DTO       entity/document
```

- **Controller:** HTTP mapping, authentication context, request validation, response status.
- **DTO:** API contract; prevents persistence shape from becoming the public contract.
- **Service:** business decisions, state transitions, authorization/ownership checks, and orchestration.
- **Entity/document:** MongoDB representation and indexes.
- **Repository:** query abstraction and persistence operations.
- **Enum:** constrained domain states such as status, category, role, or plan.
- **Tests:** executable examples of service behavior and edge cases.

## Cross-cutting path

```text
Request
 -> CORS
 -> rate limiting
 -> JWT authentication
 -> subscription enforcement
 -> controller validation
 -> service authorization/business rules
 -> repository/integration adapter
 -> exception handler or response
```

`core/` should contain reusable infrastructure and cross-cutting policies, not feature-specific business decisions. A new feature should normally be added as a module rather than placed in `core/`.

## How to navigate a ticket

| Requirement | Start looking in | Then inspect |
|---|---|---|
| Add an endpoint | Feature controller | DTO, service, API docs, tests |
| Change a business rule | Feature service | entity/enums, repository, tests |
| Add a query/filter | Repository | service authorization, indexes, API contract |
| Change login/security | `core/security`, users | `SecurityConfig`, JWT service, security tests |
| Add an external provider | `core/config` and feature adapter | timeout/error handling, secrets, integration tests |
| Add a metric/health signal | Actuator/observability config | deployment probes and dashboard |
| Change deployment behavior | `Dockerfile`, `k8s/`, compose | environment templates and CI |

## Design boundaries to defend in an interview

- Keep controllers thin and services explicit.
- Keep repositories persistence-focused; do not hide business policy in query methods.
- Keep external providers behind focused adapters/services.
- Keep tenant identity available to every service query.
- Avoid sharing mutable domain objects across modules.
- Prefer a modular monolith until deployment independence, team ownership, or scaling evidence justifies extraction.
