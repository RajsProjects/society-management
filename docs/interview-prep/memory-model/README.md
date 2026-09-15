# Memory Model

Use this model to reconstruct the project during an interview. Remember the system as six connected layers instead of memorizing every class.

## 1. The six-layer picture

```text
Client
  |
  v
HTTP + security boundary
  |  CORS, rate limit, JWT, subscription checks, validation
  v
Feature module
  |  controller -> service -> repository
  v
Domain state
  |  residents, societies, complaints, announcements, bills, payments
  v
Infrastructure
  |  MongoDB, Redis, S3, Razorpay, mail/SES
  v
Operations
     logs, metrics, health probes, Docker, Kubernetes, CI
```

The key sentence is: **a request is authenticated at the boundary, authorized in the application, changed through a service, persisted in MongoDB, and observed through operational tooling.**

## 2. The request story

For any endpoint, ask these questions in order:

1. **Who is calling?** The JWT filter loads the user identity.
2. **Are they allowed?** Security rules, method security, role checks, ownership, and subscription limits apply.
3. **What input is accepted?** A request DTO is validated before business logic.
4. **What business transition occurs?** The service checks current state and applies the domain rule.
5. **What is the source of truth?** MongoDB stores the durable business state.
6. **What side effects happen?** Redis, email, S3, Razorpay, or an event may be involved.
7. **How do we know it worked?** The response, logs, metrics, and health signals should agree.

## 3. The domain memory map

```text
Society
  +-- Users/Residents ---- authentication, approval, roles, ownership
  +-- Complaints/Issues -- status, priority, category, votes
  +-- Announcements ----- community communication
  +-- Finance ------------ maintenance bills and payment state
  +-- Subscription -------- plan limits and tenant entitlement
  +-- Communication ------- email notifications and templates
```

Remember the important state transitions:

- Resident: `SIGNUP -> PENDING -> ACTIVE` or `BLOCKED`.
- Complaint: `OPEN -> IN_PROGRESS -> RESOLVED/CLOSED`.
- Bill/payment: `GENERATED -> PENDING -> PAID/FAILED/OVERDUE`.
- Subscription: `TRIAL -> ACTIVE -> EXPIRED`, subject to plan limits.

The exact enum names are implementation details; verify them in the relevant module before quoting them.

## 4. Consistency and failure memory

MongoDB is the durable source of truth. Redis is an optimization/shared coordination store, not the only place a critical fact should exist. S3 stores large files, while MongoDB stores metadata and references. Razorpay and email are external systems and can succeed or fail independently of the database write.

Therefore, remember these rules:

- Do not report a payment as paid only because an API request was sent; verify the provider result and webhook signature.
- Do not send an email inside a critical transaction if a delayed notification is acceptable.
- Make webhook and retry handlers idempotent.
- Scope every tenant query by society/tenant identity.
- Use timeouts and bounded retries for every network integration.
- Prefer an outbox or durable job queue when a business event must never be lost.

## 5. The 30-second recall card

> “This is a modular monolith. Spring Security filters authenticate a stateless JWT request, then controllers delegate to feature services. Services enforce domain and tenant rules and use Spring Data repositories for MongoDB. Redis handles shared rate-limit/cache state, S3 handles documents, Razorpay handles payments, and email is an external side effect. Actuator, Prometheus, Grafana, Docker, and Kubernetes cover operations. The next reliability step would be durable asynchronous events with an outbox and stronger integration tests.”

## 6. How to learn a new module

Use this order every time:

1. Read the controller to list endpoints and actors.
2. Read DTOs to identify the input/output contract.
3. Read the service to identify business rules and side effects.
4. Read the entity/document and repository to understand persistence.
5. Read the tests to learn expected behavior and missing cases.
6. Trace one request with a debugger or logs.
7. Explain one failure and one scaling concern.
