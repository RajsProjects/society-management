# CivicLink Backend Architecture & Request Flow (`flowDiagram.md`)

This document provides a comprehensive end-to-end architecture and request lifecycle diagram for the **CivicLink Society Management** backend platform.

---

## 1. High-Level Architecture Flow Diagram

The diagram below shows the flow starting from the **User Block**, through network transmission, entering the **Application / Backend Block**, traversing each internal layer, interacting with persistence & external cloud services, and finally returning structured HTTP responses back to the user.

```mermaid
graph TD
    %% ==========================================
    %% USER BLOCK
    %% ==========================================
    subgraph UserBlock["👤 1. USER & CLIENT BLOCK"]
        direction TB
        ResidentUser["📱 Resident (Mobile App / Web)"]
        AdminUser["💻 Society Admin (Admin Portal)"]
        SuperAdminUser["🛡️ Platform Admin (Console)"]
        GateSecurity["🛂 Security Guard (Gate Tablet)"]
        RazorpayWebhook["💳 Razorpay Payment Webhooks"]
    end

    %% ==========================================
    %% NETWORK & REQUEST TRANSMISSION
    %% ==========================================
    UserBlock -->|"1. HTTPS Request\n• Method: GET / POST / PUT / PATCH / DELETE\n• Headers: Authorization: Bearer <JWT>\n• Headers: Content-Type: application/json\n• Body: JSON / Multipart"| ApplicationBlock

    %% ==========================================
    %% APPLICATION / BACKEND BLOCK
    %% ==========================================
    subgraph ApplicationBlock["🏢 2. APPLICATION / BACKEND BLOCK (Spring Boot 3.5 / Java 21)"]
        direction TB

        %% Ingress & Security Filters
        subgraph FilterPipeline["🛡️ A. Edge & Security Filter Pipeline"]
            direction TB
            CorsFilter["🌐 CorsFilter\nValidate Origin & Allowed Headers"]
            LoggingFilter["📝 RequestLoggingFilter\nAssign Correlation ID & Log Ingress"]
            RateLimiterFilter["⚡ RedisRateLimiterFilter\nSliding Window Rate Limit (100 req/min)"]
            JwtFilter["🔐 JwtAuthenticationFilter\n1. Extract Bearer Token\n2. Validate Signature & Expiry\n3. Load UserDetails via UserRepository\n4. Extract societyId from Claims\n5. Set SecurityContext & TenantContext"]
            SecurityFilterChain["🚦 SecurityFilterChain\nRole-Based Access Control (RBAC)\n• PERMIT_ALL: /api/v1/auth/**, /swagger-ui/**\n• ROLE_ADMIN: /api/v1/users/**, /api/v1/flats/**\n• ROLE_RESIDENT: /api/v1/complaints/**, /api/v1/issues/**"]

            CorsFilter --> LoggingFilter --> RateLimiterFilter --> JwtFilter --> SecurityFilterChain
        end

        %% Context Isolation
        subgraph TenantLayer["🏢 B. Multi-Tenant Context Engine"]
            TenantContextHolder["🧵 TenantContext (ThreadLocal)\n• Holds current societyId\n• Scopes all downstream queries\n• Guaranteed cleanup in finally block"]
        end

        %% Controllers
        subgraph ControllerLayer["🎮 C. REST API Controller Layer"]
            direction TB
            AuthController["🔑 AuthController\n/api/v1/auth"]
            AdminController["👥 AdminController\n/api/v1/users"]
            SocietyController["🏛️ SocietyController\n/api/v1/societies"]
            FlatController["🚪 FlatController\n/api/v1/flats"]
            ComplaintController["🧾 ComplaintController\n/api/v1/complaints"]
            AnnouncementController["📢 AnnouncementController\n/api/v1/announcements"]
            IssueController["🗳️ IssueController\n/api/v1/issues"]
            FinanceController["💳 FinanceController\n/api/v1/finance"]
            SubscriptionController["📦 SubscriptionController\n/api/v1/subscriptions"]
            DashboardController["📊 DashboardController\n/api/v1/dashboard/stats"]
            WebhookController["⚡ WebhookController\n/api/v1/webhooks/razorpay"]
        end

        %% Global Exception Handler
        subgraph ExceptionLayer["⚠️ D. Centralized Error Handling"]
            GlobalExceptionHandler["GlobalExceptionHandler (@RestControllerAdvice)\n• Handles MethodArgumentNotValidException (400)\n• Handles ResponseStatusException (403, 404, 409)\n• Returns Standardized JSON ErrorResponse"]
        end

        %% Business Service Layer
        subgraph ServiceLayer["⚙️ E. Business Logic & Domain Services"]
            direction TB
            AuthService["AuthService\nBCrypt + JWT Issuance + Pepper"]
            UserService["UserService\nResident Status Lifecycle"]
            SocietyService["SocietyService\nOnboarding & Join Codes"]
            FlatService["FlatService\nAllocation & Occupancy"]
            ComplaintService["ComplaintService\nCategorization & Resolution"]
            AnnouncementService["AnnouncementService\nTargeted Notices"]
            IssueService["IssueService\nCommunity Issues & Voting"]
            BillService["MaintenanceBillService\nBilling, Due Dates & UPI"]
            SubService["SubscriptionService\nPlans, Limits & Expirations"]
            WebHookService["WebhookService\nSignature Verification & Activation"]
            S3Service["S3Service\nPresigned Document/Photo URLs"]
            EventPublisher["📢 ApplicationEventPublisher\nDecoupled Async Events"]
        end

        %% Event Listeners
        subgraph AsyncListeners["📨 F. Asynchronous Event Listeners"]
            direction TB
            EmailEventListener["EmailEventListener (@Async)\nListens for: BillGeneratedEvent,\nPaymentSuccessEvent, InviteSentEvent"]
            EmailService["EmailService\nDispatches HTML Emails via SMTP/SES"]
            EmailEventListener --> EmailService
        end

        %% Persistence Repositories
        subgraph RepositoryLayer["💾 G. Spring Data Persistence Repositories"]
            direction TB
            UserRepo["UserRepository"]
            SocietyRepo["SocietyRepository"]
            FlatRepo["FlatRepository"]
            ComplaintRepo["ComplaintRepository"]
            AnnouncementRepo["AnnouncementRepository"]
            IssueRepo["IssueRepository"]
            BillRepo["MaintenanceBillRepository"]
            SubRepo["SubscriptionRepository"]
            TokenRepo["InviteTokenRepository"]
            VoteRepo["IssueVoteRepository"]
            MongoTemplate["MongoTemplate (Atomic Bulk Operations)"]
        end

        %% Link internal application layers
        SecurityFilterChain --> ControllerLayer
        JwtFilter -.->|"Populate ThreadLocal"| TenantContextHolder
        TenantContextHolder -.->|"Inject societyId"| ServiceLayer
        ControllerLayer --> ServiceLayer
        ControllerLayer -.->|"On Validation Failure"| GlobalExceptionHandler
        ServiceLayer --> RepositoryLayer
        ServiceLayer --> EventPublisher
        EventPublisher --> EmailEventListener
    end

    %% ==========================================
    %% INFRASTRUCTURE & EXTERNAL BLOCK
    %% ==========================================
    subgraph InfrastructureBlock["☁️ 3. DATABASE & CLOUD INFRASTRUCTURE"]
        direction TB

        subgraph MongoDbAtlas["🍃 MongoDB Atlas (Replica Set)"]
            UsersCol[("users collection\n(Indexed by email, societyId)")]
            SocietiesCol[("societies collection\n(Indexed by joinCode, code)")]
            FlatsCol[("flats collection\n(Compound: societyId + flatNumber)")]
            ComplaintsCol[("complaints collection\n(Compound: societyId + status)")]
            AnnouncementsCol[("announcements collection\n(Compound: societyId + createdAt)")]
            IssuesCol[("issues & votes collection")]
            BillsCol[("maintenance_bills collection\n(Compound: societyId + userId)")]
            SubsCol[("subscriptions collection")]
        end

        subgraph RedisCache["⚡ Redis (Cluster / In-Memory)"]
            RateLimitCache["Rate Limit Counters\n(TTL Sliding Window)"]
            TokenBlacklist["JWT Revocation & Cache"]
        end

        subgraph ExternalCloud["🌐 External Cloud Providers"]
            AwsS3["🪣 AWS S3\nDocument & Photo Bucket"]
            RazorpayGateway["💳 Razorpay Gateway\nOrders & Webhooks"]
            MailServer["✉️ SMTP / AWS SES\nEmail Delivery"]
        end
    end

    %% Connect application to infrastructure
    RateLimiterFilter <-->|"Incr / Window Check"| RateLimitCache
    JwtFilter <-->|"Check Revocation"| TokenBlacklist
    RepositoryLayer <-->|"Indexed B-Tree Queries"| MongoDbAtlas
    S3Service <-->|"Generate Presigned Put/Get URLs"| AwsS3
    SubService <-->|"Create Order / Fetch Payment"| RazorpayGateway
    EmailService -->|"Send Email"| MailServer

    %% ==========================================
    %% RESPONSE FLOW
    %% ==========================================
    ControllerLayer -->|"2. Return ResponseEntity<DTO>\n• Map Entity -> DTO\n• Set HTTP Status (200, 201)\n• TenantContext.clear() in finally"| ResponsePipeline
    GlobalExceptionHandler -->|"2. Return ResponseEntity<ErrorResponse>\n• Error message & timestamp\n• HTTP 400 / 401 / 403 / 404 / 500"| ResponsePipeline

    subgraph ResponsePipeline["📦 4. RESPONSE PIPELINE"]
        direction TB
        Serializer["Jackson JSON Serializer\nFormat DTO to UTF-8 JSON"]
        HttpResponse["HTTP Response\nHeaders + Status Code + Body"]
        Serializer --> HttpResponse
    end

    ResponsePipeline -->|"3. Clean JSON Output\n(e.g., 200 OK with PagedResponse or 201 Created)"| UserBlock
```

---

## 2. End-to-End Request/Response Sequence Flow

Here is the step-by-step lifecycle of an authenticated request (e.g., resident creates a complaint or admin views maintenance bills):

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 Client (App/Browser)
    participant RateLimit as ⚡ Redis RateLimiter
    participant JwtFilter as 🔐 JwtAuthFilter
    participant TenantCtx as 🧵 TenantContext (ThreadLocal)
    participant Security as 🚦 SecurityFilterChain
    participant Controller as 🎮 RestController
    participant Service as ⚙️ Business Service
    participant Repo as 💾 Mongo Repository
    participant Mongo as 🍃 MongoDB Atlas
    participant EventPub as 📢 Event Publisher
    participant Email as 📨 Async Mail Service

    User->>RateLimit: HTTP Request (Method, Path, Bearer Token, JSON Body)
    RateLimit->>RateLimit: Check Redis sliding window counter
    alt Rate Limit Exceeded
        RateLimit-->>User: 429 Too Many Requests
    else Allowed
        RateLimit->>JwtFilter: Forward request
    end

    JwtFilter->>JwtFilter: Verify JWT signature & check expiration
    alt Invalid / Expired Token
        JwtFilter-->>User: 401 Unauthorized
    else Valid Token
        JwtFilter->>TenantCtx: setSocietyId(claims.societyId)
        JwtFilter->>Security: Set SecurityContextHolder(Authentication)
    end

    Security->>Security: Validate role permission (@PreAuthorize / URL patterns)
    alt Unauthorized Role
        Security-->>User: 403 Forbidden
    else Authorized
        Security->>Controller: Dispatch to matching Controller method
    end

    Controller->>Controller: Validate @Valid DTO constraints (NotNull, Size, Pattern)
    alt DTO Validation Fails
        Controller-->>User: 400 Bad Request (Field Validation Errors)
    end

    Controller->>Service: Call service method with validated DTO & User
    Service->>TenantCtx: getSocietyId() to enforce tenant boundary
    Service->>Service: Apply business domain validations

    Service->>Repo: Execute indexed query (e.g., findBySocietyIdAndStatus)
    Repo->>Mongo: B-tree Index Scan (IXSCAN)
    Mongo-->>Repo: Document results
    Repo-->>Service: Return entity / page

    opt Asynchronous Side Effect Needed (Bill / Payment)
        Service->>EventPub: publishEvent(BillGeneratedEvent)
        EventPub-)Email: Handle asynchronously in background worker
    end

    Service-->>Controller: Return mapped Response DTO
    Controller-->>User: 200 OK / 201 Created (JSON Response Body)

    Note over JwtFilter,TenantCtx: finally { TenantContext.clear(); }
```

---

## 3. Subsystem Breakdown Inside Application Block

```
                               ┌──────────────────────────────────────────────────────────┐
                               │                    HTTP Ingress                          │
                               └────────────────────────────┬─────────────────────────────┘
                                                            │
                     ┌──────────────────────────────────────┴──────────────────────────────────────┐
                     │                                                                             │
                     ▼                                                                             ▼
       ┌───────────────────────────┐                                                 ┌───────────────────────────┐
       │     Public Endpoints      │                                                 │    Protected Endpoints    │
       │   • /api/v1/auth/signup   │                                                 │   • /api/v1/complaints    │
       │   • /api/v1/auth/login    │                                                 │   • /api/v1/finance       │
       │   • /swagger-ui.html      │                                                 │   • /api/v1/dashboard     │
       └─────────────┬─────────────┘                                                 └─────────────┬─────────────┘
                     │                                                                             │
                     ▼                                                                             ▼
       ┌───────────────────────────┐                                                 ┌───────────────────────────┐
       │     Auth Processing       │                                                 │   JwtAuthenticationFilter │
       │  • Validate join code     │                                                 │ • Validate Bearer token   │
       │  • Hash password (BCrypt) │                                                 │ • Populate TenantContext  │
       │  • Return JWT Token       │                                                 │ • Populate SecurityContext│
       └─────────────┬─────────────┘                                                 └─────────────┬─────────────┘
                     │                                                                             │
                     └──────────────────────────────────────┬──────────────────────────────────────┘
                                                            │
                                                            ▼
                                        ┌───────────────────────────────────────┐
                                        │          Controller Layer             │
                                        │   Request mapping & DTO validation    │
                                        └───────────────────┬───────────────────┘
                                                            │
                                                            ▼
                                        ┌───────────────────────────────────────┐
                                        │           Service Layer               │
                                        │   Domain logic & tenant isolation     │
                                        └───────────┬───────────────┬───────────┘
                                                    │               │
                               ┌────────────────────┘               └────────────────────┐
                               ▼                                                         ▼
                ┌─────────────────────────────┐                           ┌─────────────────────────────┐
                │   Synchronous Persistence   │                           │     Asynchronous Events     │
                │ • Spring Data Mongo Repos   │                           │ • ApplicationEventPublisher │
                │ • Compound B-tree Indexes   │                           │ • @Async Email Delivery     │
                └──────────────┬──────────────┘                           └──────────────┬──────────────┘
                               │                                                         │
                               ▼                                                         ▼
                ┌─────────────────────────────┐                           ┌─────────────────────────────┐
                │        MongoDB Atlas        │                           │       External Cloud        │
                │  users, societies, flats,   │                           │  SMTP / SES, AWS S3,        │
                │  bills, complaints, issues  │                           │  Razorpay Webhook Engine    │
                └─────────────────────────────┘                           └─────────────────────────────┘
```

---

## 4. Archify Machine-Readable Specification (`.archify` model)

Below is the structured **Archify** component and pipeline mapping corresponding to this architecture:

```json
{
  "$schema": "https://raw.githubusercontent.com/archify/spec/v1/schema.json",
  "name": "CivicLink Society Management System",
  "architecture_type": "Modular Monolith Multi-Tenant SaaS",
  "blocks": [
    {
      "id": "block-user",
      "name": "User & Client Block",
      "type": "client",
      "actors": ["Resident (Mobile/Web)", "Admin (Portal)", "Security Guard", "Razorpay Webhooks"],
      "protocols": ["HTTPS", "REST", "JSON", "Multipart"]
    },
    {
      "id": "block-ingress",
      "name": "Edge & Filter Pipeline",
      "type": "gateway",
      "filters": [
        "CorsFilter",
        "RequestLoggingFilter",
        "RedisRateLimiterFilter (100 req/min)",
        "JwtAuthenticationFilter (claims & societyId extraction)",
        "SecurityFilterChain (RBAC method security)"
      ]
    },
    {
      "id": "block-application",
      "name": "Application / Backend Block",
      "runtime": "Java 21 / Spring Boot 3.5",
      "modules": [
        { "name": "Authentication & Users", "base_path": "/api/v1/auth, /api/v1/users", "service": "AuthService, UserService" },
        { "name": "Society Management", "base_path": "/api/v1/societies", "service": "SocietyService" },
        { "name": "Flat Management", "base_path": "/api/v1/flats", "service": "FlatService" },
        { "name": "Complaints Workflow", "base_path": "/api/v1/complaints", "service": "ComplaintService" },
        { "name": "Announcements", "base_path": "/api/v1/announcements", "service": "AnnouncementService" },
        { "name": "Community Issues & Voting", "base_path": "/api/v1/issues", "service": "IssueService" },
        { "name": "Maintenance Billing", "base_path": "/api/v1/finance", "service": "MaintenanceBillService" },
        { "name": "SaaS Subscriptions", "base_path": "/api/v1/subscriptions", "service": "SubscriptionService" },
        { "name": "Admin Dashboard", "base_path": "/api/v1/dashboard/stats", "service": "DashboardController" }
      ],
      "context_engine": {
        "class": "TenantContext",
        "mechanism": "ThreadLocal<String> societyId",
        "lifecycle": "Set in JwtFilter, cleared in finally block"
      }
    },
    {
      "id": "block-persistence",
      "name": "Data & State Infrastructure",
      "stores": [
        {
          "name": "MongoDB Atlas",
          "engine": "Document Store (Replica Set)",
          "collections": ["users", "societies", "flats", "complaints", "announcements", "issues", "issue_votes", "maintenance_bills", "subscriptions", "invite_tokens"],
          "indexing": "Compound multi-tenant indexes: {societyId: 1, ...}"
        },
        {
          "name": "Redis",
          "engine": "In-Memory Key-Value",
          "use_cases": ["Rate Limiting Token Bucket", "Token Revocation", "Dashboard Cache"]
        }
      ]
    },
    {
      "id": "block-external",
      "name": "External Cloud Integrations",
      "services": [
        { "name": "AWS S3", "usage": "Society verification documents, issue photos" },
        { "name": "Razorpay", "usage": "Subscription orders, payments, webhooks" },
        { "name": "SMTP / AWS SES", "usage": "Async billing notices & welcome emails" }
      ]
    }
  ],
  "request_pipeline": [
    "Client Request -> HTTPS Ingress",
    "Ingress -> CorsFilter -> RequestLoggingFilter -> RedisRateLimiter",
    "RateLimiter -> JwtAuthenticationFilter -> TenantContext.setSocietyId()",
    "JwtFilter -> SecurityFilterChain (Role Verification)",
    "Security -> RestController (@Valid DTO validation)",
    "RestController -> Domain Service (Business Rules & Tenant Scoping)",
    "Domain Service -> Spring Data Repository -> MongoDB Atlas (Compound Index)",
    "Domain Service -> ApplicationEventPublisher -> Async Mail Listener",
    "Domain Service -> RestController -> ResponseEntity<DTO>",
    "Filter finally -> TenantContext.clear()",
    "Response Pipeline -> Jackson Serializer -> HTTP 200/201 -> Client"
  ]
}
```
