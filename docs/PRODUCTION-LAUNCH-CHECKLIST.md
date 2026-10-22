# Production Launch Checklist & Operational Runbook

This document outlines the mandatory operational and architectural controls required before opening production traffic to Samaaj / CivicLink.

---

## 1. Secrets Rotation & Hardening

### Pre-requisites
In production (`SPRING_PROFILES_ACTIVE=prod`), all sensitive secrets **must be injected via environment variables** with no default fallbacks.
The system includes `ProductionSecurityValidator`, which validates secret entropy at startup and aborts initialization if default credentials (`ChangeMe123!`, `rzp_test_dummy`, `dummy`) are present.

### Generating High-Entropy Secrets
Run the platform secret generator:
```bash
# On Linux / macOS
chmod +x scripts/generate-production-secrets.sh
./scripts/generate-production-secrets.sh

# On Windows (PowerShell)
.\scripts\generate-production-secrets.ps1
```

### Required Production Environment Variables
| Variable Name | Requirements | Description |
|---|---|---|
| `JWT_SECRET` | Min 32 chars (256-bit entropy) | HMAC key for signing and validating resident/admin JWTs |
| `SECURITY_PEPPER` | Min 16 chars (hex string) | Server-side cryptographic pepper combined with BCrypt |
| `PLATFORM_ADMIN_EMAIL` | Valid operations email | Root platform admin account (e.g. `ops@samaaj.in`) |
| `PLATFORM_ADMIN_PASSWORD` | Min 12 chars (complex) | Initial seeded password for root platform administrator |
| `RAZORPAY_KEY_ID` | `rzp_live_...` | Live Razorpay merchant key identifier |
| `RAZORPAY_KEY_SECRET` | Live secret string | Live Razorpay merchant API secret |
| `RAZORPAY_WEBHOOK_SECRET` | Live webhook secret | Webhook HMAC signature verification key |
| `REDIS_PASSWORD` | Strong password | Authentication key for production Redis cluster |

---

## 2. MongoDB Index Provisioning (Pre-Traffic Migration)

### Architecture
In production, `spring.data.mongodb.auto-index-creation` is disabled (`false`) to avoid startup latency, blocking locks, and index contention across scaled Tomcat pods.

### Execution
Run the automated index migration script before routing traffic:
```bash
mongosh "${MONGODB_URI}" scripts/mongo-indexes-migration.js
```

This creates all required unique and compound indexes in the background across:
- `users`: Email unique, tenant status & role compound indexes
- `societies`: Registration number & society code uniqueness
- `society_groups`: Strict single official group constraint per society (`{ societyId: 1 }` unique)
- `flats`: `{ societyId: 1, flatNumber: 1 }` unique constraint
- `maintenance_bills`: `{ societyId: 1, apartmentNumber: 1, billingMonth: 1 }` unique constraint, UPI transaction ID unique sparse
- `complaints`: Composite indexes for status, category, resident filtering
- `announcements`: Society feed indexes
- `issues` & `issue_votes`: Poll vote uniqueness (`{ issueId: 1, userId: 1 }`)
- `subscriptions`: Razorpay order mapping & audit history

---

## 3. Subscription Enforcement Configuration

| Setting | Values | Recommended Timing |
|---|---|---|
| `SUBSCRIPTION_ENFORCEMENT_ENABLED` | `false` | **Phase 1: Initial Launch / Society Onboarding** — Prevents lockout during data migration, initial resident invitations, and grace periods. |
| `SUBSCRIPTION_ENFORCEMENT_ENABLED` | `true` | **Phase 2: Commercial Billing Live** — Automatically blocks mutation operations (`POST`, `PUT`, `DELETE`) with `402 Payment Required` for societies with `EXPIRED` or `PAST_DUE` subscription status. |

---

## 4. Multi-Tenant Context & Virtual Thread Safety

### Isolation Architecture
- The application runs on Java 21 Virtual Threads (`spring.threads.virtual.enabled: true`).
- `TenantContextCleanupFilter` runs with `@Order(Ordered.HIGHEST_PRECEDENCE)` at the outermost layer of the servlet container.
- **Defensive Guarantee:**
  1. `TenantContext.clear()` is called before processing begins to purge any residual context on recycled carrier threads.
  2. `TenantContext.clear()` is called in an unconditional `finally` block upon completion of every HTTP request, handling both normal responses and unhandled runtime exceptions.
  3. `JwtAuthenticationFilter` wraps all token validation and security context handling within its own `try-finally` block.
  4. `AsyncConfig` wraps all `@Async` and task executor tasks in a `TaskDecorator` that guarantees `finally { TenantContext.clear(); SecurityContextHolder.clearContext(); }`.

---

## 5. Nginx TLS Termination & Reverse Proxy

### Architecture
Tomcat runs as an unprivileged HTTP service on port 8080/8081. Nginx handles:
- Public TLS 1.2 / TLS 1.3 termination on port 443.
- Automatic HTTP (port 80) to HTTPS redirection.
- Passing `X-Forwarded-Proto https` and `X-Forwarded-For` to Tomcat (`server.forward-headers-strategy: framework`).
- Rate limiting at the network edge (`60 req/s` with burst 30).
- Keepalive connection pooling to Tomcat backend.

### Quick Start
Production configuration templates are located in `deployment/nginx/`:
- `deployment/nginx/nginx.conf`: Core engine settings, logging, gzip, and edge rate limiting.
- `deployment/nginx/conf.d/society-management.conf`: Virtual host, SSL cipher suites, proxy headers, and ACME challenge paths.
- `docker-compose.prod.yml`: Turnkey production stack including Nginx, Tomcat, Redis, Prometheus, and Grafana.

---

## 6. CI/CD & Soak Testing

### Continuous Integration
- Pipeline located in `.github/workflows/ci.yml`.
- Automatically executes on pushes and pull requests to `main` and `problems`.
- Builds with Java 21 Temurin, executes the full JUnit test suite, and uploads test artifacts.

### Load and Soak Testing
Before public launch, validate sustained system stability and connection pool boundaries:
```bash
# 1. Baseline Full API Scenario Test
k6 run k6/full-test.js

# 2. Peak Traffic Stress Test (Up to 450 concurrent virtual users)
k6 run k6/stress-test.js

# 3. Sustained Endurance / Soak Test (Detects memory creep, connection leaks)
k6 run k6/soak-test.js
```
