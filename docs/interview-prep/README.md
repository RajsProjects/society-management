# Society Management Interview Preparation

This folder is a project-specific study guide for understanding, explaining, and improving the Society Management backend in an interview.

## Start here

1. Read [topics/README.md](topics/README.md) to build the required knowledge in a sensible order.
2. Use [memory-model/README.md](memory-model/README.md) to recall the system from a single request through persistence and external side effects.
3. Use [architecture/folder-architecture.md](architecture/folder-architecture.md) to navigate from a feature requirement to the right source files.
4. Use [architecture/system-design.md](architecture/system-design.md) to practise explaining the complete production design.
5. Rehearse with [architecture/interview-questions.md](architecture/interview-questions.md).

## One-minute project summary

Society Management is a modular monolithic Spring Boot REST API for residential communities. It supports authentication and resident approval, society and subscription boundaries, complaints/issues, announcements, maintenance billing, payments, email notifications, file storage, and operational monitoring. MongoDB is the primary database, Redis supports shared state and rate limiting, JWT provides stateless authentication, and the application can run as a Docker container behind Kubernetes.

The most important interview theme is the trade-off: the application has independently organized business modules inside one deployable process. This keeps development and transactions simple while leaving a path to extract high-load or independently owned modules into services later.

## Project facts to verify while studying

- Runtime: Java 21 and Spring Boot 3.5.
- Persistence: Spring Data MongoDB and MongoDB Atlas.
- Security: Spring Security, stateless JWT authentication, password peppering, method security, rate limiting, and subscription enforcement.
- Integrations: Redis, SMTP/SES email, AWS S3, and Razorpay.
- Operations: Actuator, Micrometer/Prometheus, Grafana, Docker Compose, Kubernetes manifests, and k6 tests.
- Tests: JUnit 5 and Mockito service-level tests.

When an implementation detail changes, update this guide and the source documentation together. The code is the source of truth.
