# 🏢 How Shit Goes (HSG.md): Multi-Tenancy Architecture & Failure Analysis

> **Document Codename:** `HSG.md` (*How Shit Goes*)  
> **System:** CivicLink Multi-Tenant Society Management Platform  
> **Architectural Paradigm:** Shared Database, Shared Collection with Discriminator Key (`societyId`)  
> **Date:** October 9, 2026  
> **Classification:** Internal Architectural Audit & Threat Model  

---

## 1. Executive Summary

In a software-as-a-service (SaaS) platform, **Multi-Tenancy** is the foundational guarantee that Organization A (e.g., *Green Heights Apartments*) cannot read, tamper with, or even acknowledge the existence of Organization B's data (e.g., *Silver Oak Residency*).

This document serves as an exhaustive breakdown of:
1. **How the tenant engine works right now** (The Good).
2. **Where and how the system can fail catastrophically** (The Bad).
3. **The systemic loopholes that can cause cross-tenant data leaks** (The Ugly).
4. **The architectural roadmap to reach an unbreakable 100% guarantee.**

---

## 2. Current Multi-Tenancy Architecture (How It Works Today)

The application currently employs **Logical Multi-Tenancy (Application-Layer Field Discrimination)**. All tenants share the same MongoDB database, the same collections (`users`, `flats`, `maintenance_bills`, `complaints`, etc.), and the same connection pool. Isolation is maintained strictly through application code conventions.

### Architectural Request Pipeline

```
[ Incoming HTTP Request ]
          │ (Authorization: Bearer <JWT>)
          ▼
┌──────────────────────────────────────────────────────────────┐
│ 1. JwtAuthenticationFilter                                   │
│    • Parses cryptographic JWT signature                      │
│    • Extracts 'societyId' claim from JWT                     │
│    • Validates account status (REJECTS BLOCKED / INACTIVE)   │
│    • Stores societyId in TenantContext                       │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│ 2. SubscriptionEnforcementFilter (Tenant Status Gatekeeper)  │
│    • Checks society.getStatus() == ACTIVE                    │
│      ↳ If SUSPENDED or REJECTED: HTTP 403 Forbidden          │
│    • Checks society.getSubscriptionStatus()                  │
│      ↳ If EXPIRED or PAST_DUE: HTTP 402 on write operations  │
│    • Exempts PLATFORM_ADMIN & reactivation routes            │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│ 3. Spring MVC Controller Layer                               │
│    • Enforces Method Security (@PreAuthorize)                │
│    • Injects @AuthenticationPrincipal User currentUser      │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│ 4. Service Layer (Tenant Isolation Enforced by Hand)         │
│    • Calls requireSocietyId() -> TenantContext.getSocietyId()│
│    • Executes tenant-scoped repository queries               │
│      e.g., findByIdAndSocietyId(id, societyId)               │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│ 5. MongoDB Atlas (Shared Database / Shared Collection)       │
│    • Filtered by compound index: {'societyId': 1, ...}       │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│ 6. Response Lifecycle & Thread Cleanup                       │
│    • finally { TenantContext.clear(); }                      │
│    • Prevents memory leakage across Tomcat pooled threads    │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. Deep Dive into the 4 Core Moving Parts

### 3.1. The In-Memory Tenant Carrier: `TenantContext.java`
Tenant state is held in a `ThreadLocal` wrapper:

```java
public class TenantContext {
    private static final ThreadLocal<String> CURRENT_SOCIETY = new ThreadLocal<>();

    public static void setSocietyId(String societyId) {
        CURRENT_SOCIETY.set(societyId);
    }

    public static String getSocietyId() {
        return CURRENT_SOCIETY.get();
    }

    public static void clear() {
        CURRENT_SOCIETY.remove();
    }
}
```

* **Purpose:** Allows any deep business service or helper component to retrieve the currently active tenant ID without passing `societyId` manually through hundreds of method parameters.
* **Scope:** Tied exclusively to the single operating thread executing the HTTP request.

---

### 3.2. Edge Extraction & Thread Hygiene: `JwtAuthenticationFilter.java`
Located at the security boundary:

```java
// Extract tenant claim directly from verified JWT
String societyId = jwtService.extractSocietyId(token);
if (societyId == null && userDetails instanceof User user) {
    societyId = user.getSocietyId();
}
TenantContext.setSocietyId(societyId);

try {
    filterChain.doFilter(request, response);
} finally {
    // MANDATORY THREAD-POOL HYGIENE:
    // Web servers like Tomcat reuse worker threads. If clear() is not called,
    // subsequent unauthenticated requests or requests with bad tokens
    // could inherit the previous user's tenant context!
    TenantContext.clear();
}
```

---

### 3.3. Tenant Lifecycle Gatekeeping: `SubscriptionEnforcementFilter.java`
Prevents orphaned or suspended societies from continuing to execute operations:

1. **Status Checks:** Verifies `society.getStatus() == SocietyStatus.ACTIVE`. If an administrator or platform owner suspends a society due to non-compliance or fraud, existing unexpired user JWTs are blocked immediately with **HTTP 403 Forbidden**.
2. **SaaS Billing Checks:** If the society's subscription is `EXPIRED` or `PAST_DUE`, mutating requests (`POST`, `PUT`, `PATCH`, `DELETE`) are blocked with **HTTP 402 Payment Required**, while keeping historical records read-only viewable (`GET`).

---

### 3.4. Service-Level Scoping: Manual `requireSocietyId()`
Every secure service method manually extracts and bounds its queries:

```java
private String requireSocietyId() {
    String societyId = TenantContext.getSocietyId();
    if (societyId == null || societyId.isBlank()) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No society context");
    }
    return societyId;
}

// Scoped retrieval:
MaintenanceBill bill = billRepository.findByIdAndSocietyId(billId, societyId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
```

---

## 4. How Shit Breaks: The Gaps & Vulnerabilities

While the current system functions properly across the current test suite, **it does NOT guarantee multi-tenancy unconditionally**. Here is exactly how and why shit goes wrong in real-world production environments:

---

### 💥 Failure Mode 1: The "Developer Discipline" Fallacy (No Automated DB Interceptor)

#### The Problem:
Spring Data MongoDB has **zero intrinsic knowledge** of multi-tenancy. It executes whatever queries are defined in the repository.

#### How It Breaks:
If an engineer onboards to the team tomorrow, builds a new feature (e.g., `VisitorPassService`), and writes:

```java
// VULNERABLE CODE:
@GetMapping("/visitors/{id}")
public VisitorPass getPass(@PathVariable String id) {
    // The developer forgot to call requireSocietyId() or use findByIdAndSocietyId()!
    return visitorPassRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
}
```

**The Impact:**
A resident from *Society B* can simply pass a valid UUID from *Society A* and read confidential visitor logs, license plates, and guest phone numbers. **The database engine will not stop them.**

*Live example found in the codebase:*
In `UserService.java:71`, the legacy method:
```java
public List<UserResponseDto> getAllUsers() {
    return userRepository.findAll().stream().map(UserResponseDto::from).toList();
}
```
This method executes an unconstrained `findAll()`, which queries across **every single society in the entire database**.

---

### 💥 Failure Mode 2: ThreadLocal Silo in Asynchronous / Background Tasks

#### The Problem:
`TenantContext` uses standard Java `ThreadLocal<String>`.

#### How It Breaks:
`ThreadLocal` values are stored in the memory space of the **calling thread only**. If any operation is handed off to a background thread pool, `TenantContext` evaporates:
* `@Async` methods
* `CompletableFuture.supplyAsync(...)`
* Parallel Streams (`list.parallelStream()`)
* Scheduled Crons (`@Scheduled`)
* Spring Event Listeners (`@EventListener` or `@TransactionalEventListener`)

```java
@Async
public void processReportGeneration(String reportId) {
    // RUNS ON TASK-EXECUTOR THREAD:
    String societyId = TenantContext.getSocietyId(); // -> RETURNS NULL!
    
    // Throws 403 Forbidden or crashes with NullPointerException!
}
```

*The Midnight Cron Example:*
In `EmailService.java` or `MaintenanceBillService.java`, the scheduled cron `sendOverdueNotifications()` runs at 00:00 UTC without an incoming HTTP request. `TenantContext.getSocietyId()` is permanently `null`. The system has to bypass tenancy and iterate over global database queries.

---

### 💥 Failure Mode 3: Relational BOLA & Cross-Tenant Foreign Key Injection

#### The Problem:
Entities frequently reference other entities (e.g., a `MaintenanceBill` references a `userId` and `apartmentNumber`; a `Flat` references an `ownerId`).

#### How It Breaks:
If the API creates or mutates a record in Tenant A, but accepts a raw ID pointing to Tenant B:

```java
// POST /api/v1/finance/bills
// Body: { "userId": "user-from-society-B", "apartmentNumber": "A-101", "amount": 5000 }
```

If the service only validates that the *administrator* belongs to Society A, but loads the target user using `userRepository.findById(request.getUserId())` (without `societyId`), **Tenant A just created a financial liability on a resident belonging to Tenant B!**

---

### 💥 Failure Mode 4: Shared Database & Physical Blast Radius

#### The Problem:
All tenants share the same physical MongoDB database and collections.

#### How It Breaks:
1. **The "Noisy Neighbor" Problem:** If Society A has 5,000 residents actively voting on an issue, their heavy queries saturate the shared MongoDB connection pool (`max-size: 50`), degrading latency and timing out requests for Society B.
2. **Database Dump Leakage:** If a developer makes a backup mistake, runs an unindexed query in MongoDB Compass, or leaks read-only MongoDB credentials, **the entire customer base across all societies is compromised simultaneously.**
3. **No Per-Tenant Data Residency / Cryptographic Purge:** If Society A leaves the SaaS platform and requests full GDPR/DPDP data erasure, the platform cannot simply drop a database. It must execute fragile deletion cascades across dozens of shared collections.

---

## 5. The Multi-Tenancy Maturity Matrix

| Capability | Level 1: Current Architecture | Level 2: Automated Query Interceptor | Level 3: Database-per-Tenant |
|---|---|---|---|
| **Enforcement Layer** | Application code (`requireSocietyId`) | Spring Data Mongo AOP / `EntityCallback` | Physical DB Connection Routing |
| **Developer Error Resistance** | ❌ **Low** (one forgotten check leaks data) | 🛡️ **High** (impossible to forget) | 🔒 **Absolute** (physically isolated) |
| **Query Automatic Filtering** | ❌ Manual (`findByIdAndSocietyId`) | 🛡️ Automatic (injects `{societyId: X}`) | 🔒 Automatic (queries isolated DB) |
| **Noisy Neighbor Protection** | ❌ None (shared pool & CPU) | ❌ None (shared pool & CPU) | 🛡️ High (isolated quotas/databases) |
| **Data Breach Blast Radius** | ❌ All tenants compromised | ❌ All tenants compromised | 🛡️ Single tenant only |
| **Operational Overhead** | 🟢 Minimal | 🟢 Low | 🟡 Medium (DB provisioning) |

---

## 6. How We Fix It (The Production Hardening Roadmap)

To evolve the architecture into a secure, enterprise-grade multi-tenant engine without the complexity of managing thousands of separate databases, we must implement **Level 2: Automated Database-Level Interception**.

### Roadmap Step 1: Automated MongoDB Query Interceptor
Instead of trusting developers to append `findBySocietyId...`, wrap `MongoTemplate` or register a Spring Data MongoDB `BeforeConvertCallback` and `QueryInterceptor`:

```java
// Concept: Automatic Tenant Injection on EVERY MongoDB Query
@Component
public class TenantAwareMongoEventListener extends AbstractMongoEventListener<BaseEntity> {

    @Override
    public void onBeforeConvert(BeforeConvertEvent<BaseEntity> event) {
        BaseEntity entity = event.getSource();
        String currentTenant = TenantContext.getSocietyId();
        
        if (currentTenant != null && entity.getSocietyId() == null) {
            entity.setSocietyId(currentTenant);
        }
    }
}
```

### Roadmap Step 2: Context Propagation to Async Threads
Configure Spring's `TaskDecorator` so that `TenantContext` automatically transfers from the request thread into thread pools:

```java
@Configuration
public class AsyncConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(runnable -> {
            String tenantId = TenantContext.getSocietyId();
            return () -> {
                try {
                    TenantContext.setSocietyId(tenantId);
                    runnable.run();
                } finally {
                    TenantContext.clear();
                }
            };
        });
        return executor;
    }
}
```

### Roadmap Step 3: Purge All Unscoped Legacy Methods
Deprecate and delete any repository method that queries entities without tenant boundaries (such as `userRepository.findAll()` in `UserService`).

---

## 7. Architectural Verdict

> **Is the current multi-tenancy working?**  
> **Yes.** For all currently mapped endpoints and test suites, the request pipeline and tenant-scoped lookups are operating as intended.
>
> **Is multi-tenancy guaranteed always?**  
> **No.** It is a *logical* abstraction relying entirely on human discipline. Without automated query interception and async context propagation, it remains vulnerable to developer oversights, un-scoped queries, and async context drops.
