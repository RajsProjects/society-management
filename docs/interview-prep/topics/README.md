# Topics to Learn

Use this as a checklist. For each topic, be able to explain the concept, point to the relevant code, describe one failure mode, and suggest one improvement.

## Phase 1: Application and Java fundamentals

- [ ] Java 21: records, enums, streams, Optional, exceptions, immutability, and concurrency basics.
- [ ] Spring Boot startup and component scanning.
- [ ] Dependency injection, bean lifecycle, profiles, and externalized configuration.
- [ ] REST semantics: resources, status codes, validation, pagination, idempotency, and error responses.
- [ ] Lombok and the risks of generated constructors, builders, and equality methods.

## Phase 2: Read one feature end to end

Trace an announcement or complaint request:

`HTTP request -> filter chain -> controller -> DTO validation -> service -> repository -> MongoDB -> response`

- [ ] Find the controller, request/response DTOs, service, entity/document, repository, and tests.
- [ ] Explain why DTOs are preferable to exposing persistence documents.
- [ ] Explain where authorization belongs and why it must not rely only on the UI.
- [ ] Explain how domain exceptions become HTTP responses.

## Phase 3: Security

- [ ] Signup, login, password hashing, password peppering, and credential storage.
- [ ] JWT claims, signing, expiry, validation, and stateless request authentication.
- [ ] `SecurityFilterChain` order and the difference between authentication and authorization.
- [ ] Role checks for ADMIN and RESIDENT, plus ownership checks for resident data.
- [ ] CORS, CSRF implications for a stateless API, and secure cookie versus bearer-token trade-offs.
- [ ] Rate limiting and what changes when the application has multiple replicas.
- [ ] Secret management: environment variables, Kubernetes Secrets, rotation, and least privilege.

## Phase 4: Data and domain design

- [ ] MongoDB document modeling, references versus embedding, indexes, and query selectivity.
- [ ] Tenant/society isolation: how every read and write is scoped to the current society.
- [ ] Resident lifecycle: pending approval, active, blocked, and the allowed transitions.
- [ ] Complaint lifecycle, voting, announcements, bills, payment status, and auditability.
- [ ] Atomicity boundaries in MongoDB and what cannot be assumed across external systems.
- [ ] Idempotency keys and duplicate webhook/payment handling.
- [ ] Cache-aside design with Redis, TTLs, invalidation, and stale data.

## Phase 5: Integrations and asynchronous work

- [ ] Email events and why sending email should not delay the main API transaction.
- [ ] SMTP versus SES, retry strategy, timeouts, and dead-letter handling.
- [ ] S3 presigned URLs and keeping file bytes out of the application server.
- [ ] Razorpay order/payment/webhook flow and signature verification.
- [ ] Outbox pattern as a future improvement for reliable domain events.
- [ ] Retry safety: distinguish transient failures from permanent validation failures.

## Phase 6: Reliability, performance, and operations

- [ ] Connection pools, request threads, timeouts, backpressure, and graceful failure.
- [ ] Actuator health/readiness/liveness and what each probe should prove.
- [ ] Prometheus metrics, Grafana dashboards, structured logs, correlation IDs, and alerting.
- [ ] Docker image lifecycle and environment-specific configuration.
- [ ] Kubernetes Deployment, Service, ConfigMap, Secret, HPA, and rolling updates.
- [ ] Horizontal scaling constraints: stateless API, shared Redis, MongoDB capacity, and external rate limits.
- [ ] k6 load tests: workload model, bottleneck identification, and meaningful success criteria.

## Phase 7: Testing and delivery

- [ ] Unit tests with Mockito: service behavior, validation, authorization, and failure cases.
- [ ] Controller/integration tests with Spring Security test support.
- [ ] Contract tests for Razorpay, email, and S3 adapters.
- [ ] Test data isolation and avoiding tests that depend on shared external services.
- [ ] CI pipeline stages and how a failing test should block deployment.
- [ ] Safe rollout, rollback, database compatibility, and migration strategy.

## Suggested practice routine

For each unchecked item, write a five-line note:

1. What problem does this solve?
2. Where is it implemented in this project?
3. What happens on failure?
4. How does it behave with two application replicas?
5. What would I improve next?

Then explain the answer aloud in under two minutes. Mark the item complete only after you can do that without opening the code.
