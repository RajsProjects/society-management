# 🏢 CivicLink — Multi-Tenant Society Management SaaS Platform

<p align="center">
  <img src="docs/logo.png" width="150" alt="CivicLink Logo"/>
</p>

<p align="center">
  <b>Production-ready SaaS platform for modern residential societies</b>
</p>

<p align="center">

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-brightgreen)
![Redis](https://img.shields.io/badge/Redis-Cache-red)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Deployed-326CE5)
![Tests](https://img.shields.io/badge/Tests-101_Passing-success)
![License](https://img.shields.io/badge/License-MIT-yellow)

</p>

---

# 🚀 Overview

CivicLink is a **multi-tenant Society Management SaaS platform** built to help apartment communities manage residents, maintenance billing, complaints, announcements, and issue tracking from a single dashboard.

The platform is designed using modern cloud-native practices:

* 🔐 JWT Authentication & Role-Based Access
* 🏢 Multi-Tenant Architecture
* 💳 Maintenance Billing System
* 📢 Announcement Management
* 🗳️ Community Issue Voting
* 🧾 Complaint Workflow Management
* 📧 Async Email Notifications
* ⚡ Redis Rate Limiting
* 📈 Prometheus + Grafana Monitoring
* 🐳 Docker & Kubernetes Ready
* 🔄 CI/CD Automation

---

# 🌟 Key Features

## Authentication & Security

* JWT Authentication
* Spring Security
* Role Based Access Control (RBAC)

### Roles

| Role        | Permissions                   |
| ----------- | ----------------------------- |
| SUPER_ADMIN | Platform Management           |
| ADMIN       | Society Administration        |
| ACCOUNTANT  | Billing & Finance             |
| SECURITY    | Visitor & Security Operations |
| RESIDENT    | Resident Access               |

---

## Society Management

* Society Registration
* Resident Approval Workflow
* Unit / Flat Management
* Tenant Isolation
* Society-Specific Data Access

---

## Billing & Finance

* Maintenance Bill Generation
* Due Date Tracking
* Payment Status Monitoring
* UPI Payment Simulation
* Automated Email Notifications

---

## Community Engagement

### Announcements

* General Notices
* Maintenance Alerts
* Emergency Notifications

### Issues

* Community Issue Reporting
* Voting System
* Issue Prioritization

### Complaints

* Complaint Creation
* Status Tracking
* Admin Resolution Workflow

---

## Invitation System

* Secure Resident Invitations
* Email-based Invite Links
* Token Validation
* Controlled Onboarding

---

# 🏗️ System Architecture

```text
                        ┌─────────────────┐
                        │     Browser     │
                        │ React + Vite UI │
                        └────────┬────────┘
                                 │
                                 ▼
                     ┌─────────────────────┐
                     │     API Gateway     │
                     │ Spring Boot Backend │
                     └─────────┬───────────┘
                               │
      ┌────────────────────────┼────────────────────────┐
      │                        │                        │
      ▼                        ▼                        ▼

┌──────────────┐      ┌────────────────┐      ┌────────────────┐
│ Authentication│      │ Society Module │      │ User Management│
│ JWT Security  │      │ Multi-Tenancy  │      │ RBAC           │
└──────┬───────┘      └──────┬─────────┘      └────────┬───────┘
       │                     │                         │
       └──────────────┬──────┴───────────────┬─────────┘
                      │                      │
                      ▼                      ▼

             ┌────────────────┐    ┌──────────────────┐
             │ Billing Module │    │ Complaint Module │
             └───────┬────────┘    └────────┬─────────┘
                     │                      │
                     ▼                      ▼

             ┌────────────────┐    ┌──────────────────┐
             │ Issues Module  │    │ Announcement Mod │
             └───────┬────────┘    └────────┬─────────┘
                     │                      │
                     └──────────┬───────────┘
                                │
                                ▼

                   ┌────────────────────┐
                   │ Async Email Service│
                   │ Thymeleaf + SMTP   │
                   └─────────┬──────────┘
                             │

          ┌──────────────────┼──────────────────┐
          ▼                  ▼                  ▼

   ┌────────────┐    ┌─────────────┐    ┌─────────────┐
   │ MongoDB    │    │ Redis Cache │    │ Prometheus  │
   │ Atlas      │    │ Rate Limit  │    │ Metrics     │
   └────────────┘    └─────────────┘    └──────┬──────┘
                                                │
                                                ▼

                                          ┌─────────┐
                                          │ Grafana │
                                          └─────────┘
```

---

# 🏢 Multi-Tenant Architecture

Every request is isolated using a **societyId**.

```text
Society A
└── Users
└── Bills
└── Issues
└── Complaints
└── Announcements

Society B
└── Users
└── Bills
└── Issues
└── Complaints
└── Announcements
```

### Tenant Isolation Flow

```text
JWT Token
    │
    ▼
Contains societyId
    │
    ▼
TenantContext (ThreadLocal)
    │
    ▼
Service Layer
    │
    ▼
MongoDB Queries
    │
    ▼
Data filtered by societyId
```

✅ Zero Data Leakage

---

# 🔄 Request Lifecycle

```text
Client Request
      │
      ▼
JWT Authentication Filter
      │
      ▼
Extract societyId
      │
      ▼
TenantContext Setup
      │
      ▼
Controller
      │
      ▼
Service Layer
      │
      ▼
MongoDB Repository
      │
      ▼
Response
```

---

# 📦 Tech Stack

| Layer            | Technology            |
| ---------------- | --------------------- |
| Language         | Java 21               |
| Framework        | Spring Boot 3.5       |
| Security         | Spring Security + JWT |
| Database         | MongoDB Atlas         |
| Cache            | Redis                 |
| Frontend         | React + Vite          |
| Styling          | Tailwind CSS          |
| Email            | Thymeleaf             |
| Containerization | Docker                |
| Orchestration    | Kubernetes            |
| Monitoring       | Prometheus            |
| Dashboards       | Grafana               |
| Testing          | JUnit 5 + Mockito     |
| Load Testing     | k6                    |
| CI/CD            | GitHub Actions        |

---

# 📈 Performance Testing

## k6 Load Test Results

| Scenario       | Users  | Avg Response Time | Error Rate |
| -------------- | ------ | ----------------- | ---------- |
| Authentication | 50     | 472ms             | 0%         |
| Complete Flow  | 200    | 1.88s             | 0%         |
| Total Requests | 17,922 | —                 | 0%         |

### Highlights

* ✅ 200 Concurrent Users
* ✅ Zero Failures
* ✅ Stable Response Times
* ✅ Production Ready

---

# 🧪 Testing

### Unit Tests

```bash
101 Tests Passing
```

Coverage includes:

* Authentication
* User Management
* Society Services
* Billing Services
* Issue Services
* Multi-Tenant Enforcement

Run tests:

```bash
./mvnw test
```

---

# 📊 Observability

### Metrics

* JVM Metrics
* HTTP Metrics
* Database Metrics
* Request Throughput
* Error Rates

### Monitoring Stack

```text
Spring Boot
      │
      ▼
Actuator
      │
      ▼
Prometheus
      │
      ▼
Grafana
```

---

# 🐳 Docker Setup

```bash
docker-compose up -d
```

Build image:

```bash
docker build -t civiclink .
```

---

# ☸️ Kubernetes Deployment

```bash
kubectl apply -f k8s/
```

Resources Included:

* Deployment
* Service
* HPA
* ConfigMaps

---

# 🚀 CI/CD Pipeline

```text
Push to GitHub
       │
       ▼
GitHub Actions
       │
       ▼
Build
       │
       ▼
Unit Tests
       │
       ▼
Docker Build
       │
       ▼
Deploy
```

---

# 📸 Screenshots

```text
docs/
 ├── login.png
 ├── dashboard.png
 ├── complaints.png
 ├── billing.png
 └── announcements.png
```

---

# 🛣️ Roadmap

## Current

* [x] Multi-Tenant SaaS Foundation
* [x] Redis Rate Limiting
* [x] Flat Management
* [x] Invitation Workflow
* [x] Role Management

## In Progress

* [ ] Complaint Workflow
* [ ] React Dashboard
* [ ] Analytics Dashboard

## Future

* [ ] Visitor Management
* [ ] Parking Management
* [ ] WhatsApp Notifications
* [ ] Payment Gateway Integration
* [ ] Mobile App

---

# 🤖 GitHub Copilot Usage

Copilot was used as an AI pair programmer for:

* Entity generation
* DTO generation
* Unit tests
* Email templates
* Boilerplate reduction

All generated code was reviewed, modified, and integrated manually.

---

# 👨‍💻 Author

**Raj Mandal**

* GitHub: https://github.com/RajsProjects
* Project: CivicLink

---

# 🏆 GitHub Finish-Up-A-Thon

Built and completed as part of the GitHub Finish-Up-A-Thon challenge.

Demonstrates:

* SaaS Architecture
* Multi-Tenancy
* Security
* Scalability
* Observability
* Cloud-Native Deployment
* Production Engineering Practices

⭐ If you found this project interesting, consider starring the repository.
