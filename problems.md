# Security and Production Issue Review

Repository reviewed: `SocietyManagement`
Review date: 2026-09-24

## Validation

- `.\mvnw.cmd -q test` completed successfully.
- Passing tests do not cover the authorization and deployment configuration problems listed below.
- Findings are based on source/configuration review and are limited to issues with a credible production or security impact.

## Findings

### 1. HIGH - User administration is not tenant-scoped

- **File:** `src/main/java/com/Application/SocietyManagement/users/service/UserService.java:25-55`
- **Confidence:** 10/10
- **Problem:** User status updates use global `findById`, and user listing uses global `findAll`/`findByStatus`/`findByRole` queries. The authenticated administrator's society is never compared with the target user's `societyId`.
- **Impact:** An administrator can enumerate residents from other societies and activate, deactivate, or block their accounts if a user ID is known. This is cross-tenant disclosure and unauthorized account disruption.
- **Recommended fix:** Add society-qualified repository methods and require the target user's society to match `TenantContext.getSocietyId()`. Keep platform-wide access behind an explicit platform-admin path.

### 2. HIGH - Maintenance bills are not tenant-scoped

- **File:** `src/main/java/com/Application/SocietyManagement/finance/service/MaintenanceBillService.java:38-88`
- **Confidence:** 10/10
- **Problem:** Bill creation loads any user globally and checks only the apartment number. Administrative listing uses global bill queries. Bills do not carry or enforce a society boundary.
- **Impact:** An administrator can create bills for residents in another society and view unrelated financial records.
- **Recommended fix:** Store `societyId` on every bill, derive it from the authenticated tenant, verify the target user belongs to that tenant, and use society-qualified queries for creation, listing, and payment.

### 3. HIGH - Issue operations by ID bypass tenant isolation

- **File:** `src/main/java/com/Application/SocietyManagement/issue/service/IssueService.java:85-128`
- **Confidence:** 9/10
- **Problem:** Status updates, priority updates, voting, and issue lookup use `issueRepository.findById(issueId)` without checking `TenantContext.getSocietyId()`.
- **Impact:** Residents can vote on another society's issues and administrators can modify them, corrupting data and community vote results.
- **Recommended fix:** Use `findByIdAndSocietyId` (or an equivalent tenant-qualified query) for every issue-by-ID operation, including vote creation and removal.

### 4. HIGH - Announcement operations bypass tenant isolation

- **File:** `src/main/java/com/Application/SocietyManagement/communication/service/AnnouncementService.java:62-84`
- **Confidence:** 9/10
- **Problem:** Individual announcement retrieval, update, and deletion use an unscoped ID lookup even though list queries are society-filtered.
- **Impact:** A user who obtains an announcement ID can read another society's announcement; an administrator can update or delete it.
- **Recommended fix:** Require the current tenant in all individual announcement lookups and use `findByIdAndSocietyId`.

### 5. HIGH - Complaint authorization does not enforce society ownership

- **File:** `src/main/java/com/Application/SocietyManagement/complaint/service/ComplaintService.java:109-187`
- **Confidence:** 9/10
- **Problem:** `findComplaint` performs a global lookup. `validateAccess` grants all administrators access without comparing society IDs, and `updateStatus` does not call `validateAccess` at all.
- **Impact:** Administrators can read, resolve, reject, or delete complaints belonging to another society. Complaint descriptions may contain sensitive resident information.
- **Recommended fix:** Enforce `complaint.societyId == TenantContext.getSocietyId()` for every operation, including status updates, and prefer tenant-qualified repository methods.

### 6. MEDIUM - Subscription management endpoints lack role authorization

- **File:** `src/main/java/com/Application/SocietyManagement/subscription/controller/SubscriptionController.java:19-32`
- **Confidence:** 9/10
- **Problem:** Create-order, verify-payment, and change-plan endpoints have no `@PreAuthorize` restriction. They are available to any authenticated user under the global security configuration.
- **Impact:** A resident can initiate or change the society subscription plan and trigger unauthorized billing or plan activation.
- **Recommended fix:** Restrict these methods to the appropriate society administrator role and verify every order belongs to the caller's tenant.

### 7. HIGH - Plaintext production credentials are present in `.env`

- **File:** `.env:1-16`
- **Confidence:** 10/10
- **Problem:** The working tree contains MongoDB, JWT, mail, AWS, Razorpay, admin-password, and security-pepper credentials in plaintext. The file is ignored by Git, but it remains exposed to anyone or any process with workspace access and may already have been copied into backups, logs, or developer machines.
- **Impact:** Compromise of databases, cloud storage, payment integration, administrator access, and token signing. A leaked JWT secret also allows forged authentication tokens.
- **Recommended fix:** Rotate every value immediately, invalidate existing JWTs, remove the file from all shared locations/history if it was ever committed, and use a secret manager or deployment secret store. Never include actual secret values in issue trackers.

### 8. HIGH - Kubernetes secret template is incomplete for the production profile

- **File:** `k8s/secret-template.yaml:1-10`, `src/main/resources/application-prod.yml:1-101`
- **Confidence:** 9/10
- **Problem:** The Kubernetes secret template supplies only `MONGODB_URI` and `JWT_SECRET`, while the production configuration requires `MAIL_USERNAME`, `MAIL_APP_PASSWORD`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `PLATFORM_ADMIN_EMAIL`, `PLATFORM_ADMIN_PASSWORD`, and `SECURITY_PEPPER`.
- **Impact:** A deployment created from the documented template can fail at startup because unresolved placeholders are required by beans such as `SecurityConfig` and `AdminSeeder`; or it can fail later when integrations are used.
- **Recommended fix:** Add all required values to a real external secret mechanism (not necessarily to a committed template), validate required configuration at startup, and document the deployment contract.

### 9. HIGH - Payment verification uses the webhook secret

- **File:** `src/main/java/com/Application/SocietyManagement/subscription/service/SubscriptionService.java:24-28, 83-112`
- **Confidence:** 9/10
- **Problem:** Client payment verification calls `Utils.verifyPaymentSignature` with `razorpay.webhook-secret`. Razorpay payment signature verification requires the Razorpay API key secret; the webhook secret is for webhook payload verification.
- **Impact:** Legitimate payments can fail verification in production, leaving subscriptions pending or marked failed after the customer has paid. If the secrets happen to be reused, separating trust boundaries is lost.
- **Recommended fix:** Inject and use `razorpay.key-secret` for client payment verification, retain `razorpay.webhook-secret` only for webhook verification, and add an integration test with distinct secrets.

### 10. MEDIUM - Actuator health details are exposed

- **File:** `src/main/resources/application.yaml:55-62`, `src/main/resources/application-prod.yml:45-52`
- **Confidence:** 8/10
- **Problem:** `management.endpoint.health.show-details` is set to `always`, while `/actuator/health` is explicitly permitted without authentication.
- **Impact:** Public callers may learn internal health-component state and operational details useful for reconnaissance or outage diagnosis.
- **Recommended fix:** Set `show-details: never` or `when-authorized`, expose only liveness/readiness publicly, and protect detailed health and metrics endpoints.

### 11. MEDIUM - Rate limiting trusts client-controlled `X-Forwarded-For`

- **File:** `src/main/java/com/Application/SocietyManagement/core/security/RateLimitingFilter.java:35-72`
- **Confidence:** 8/10
- **Problem:** The authentication rate-limit key uses the first `X-Forwarded-For` value without confirming that the request came through a trusted proxy.
- **Impact:** An attacker can rotate this header to bypass the login rate limit, enabling password-guessing and account enumeration attempts.
- **Recommended fix:** Configure trusted proxy handling and use the framework's resolved client address. Ignore forwarding headers from untrusted direct clients.

### 12. MEDIUM - Redis and monitoring services are exposed without production hardening

- **File:** `docker-compose.yml:13-52`
- **Confidence:** 9/10
- **Problem:** Redis is published on host port 6379 with no password or ACL, and Grafana uses the default `admin/admin123` credentials. Prometheus and Grafana are also published directly.
- **Impact:** Anyone able to reach the host can read or modify Redis data, potentially affecting rate limits and application state, and can access the monitoring stack with default credentials.
- **Recommended fix:** Do not publish internal services publicly; require Redis authentication/TLS, replace default Grafana credentials through secrets, and place monitoring behind authenticated network access.

### 13. MEDIUM - CORS allows every origin

- **File:** `src/main/java/com/Application/SocietyManagement/core/config/SecurityConfig.java:48-56`
- **Confidence:** 8/10
- **Problem:** `allowedOrigins` is configured as `*` for all routes.
- **Impact:** Any website can issue cross-origin requests to the API. If authentication handling changes to cookies or credentials are enabled later, this becomes a direct cross-site request risk; it also permits untrusted frontends to consume public API responses.
- **Recommended fix:** Configure an explicit allowlist of deployed frontend origins and keep credential behavior deliberate.

## Priority order

1. Rotate all exposed credentials and invalidate tokens.
2. Fix tenant scoping for users, bills, issues, announcements, and complaints.
3. Correct Razorpay payment verification secret usage.
4. Fix the Kubernetes production secret contract and add startup configuration validation.
5. Harden actuator, Redis, Grafana, rate limiting, and CORS exposure.
