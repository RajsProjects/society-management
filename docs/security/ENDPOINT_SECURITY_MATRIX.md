# Endpoint Security & Access Control Matrix

This matrix documents the comprehensive inventory of actual API endpoints implemented in the CivicLink Society Management backend, derived from direct source code inspection of all controller mappings and Spring Security filter chains.

---

## 1. Authentication & Identity Endpoints (`/api/v1/auth`)

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/signup` | **No** (Public) | Anyone | Assigns role `RESIDENT`, status `PENDING`, requires active society join code | `@Valid SignupRequest` (Email format, password complexity: 8+ chars, upper, lower, digit) | `201 Created` | `AuthenticationSecurityTest`, `InputValidationSecurityTest` |
| `POST` | `/api/v1/auth/login` | **No** (Public) | Anyone | Authenticates email & peppered BCrypt hash; verifies status is `ACTIVE` (rejects `PENDING`, `INACTIVE`, `BLOCKED`) | `@Valid LoginRequest` (Email, Password required) | `200 OK` (JWT) / `401` / `403` | `AuthenticationSecurityTest`, `RateLimitingSecurityTest` |
| `POST` | `/api/v1/auth/invite` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Scoped to caller's `societyId`; generates single-use invitation token | `@Valid InviteRequest` (Email, Role, FlatId) | `200 OK` | `PlatformAuthorizationPenetrationTest` |
| `POST` | `/api/v1/auth/accept-invite`| **No** (Public) | Anyone | Single-use invite token verified and invalidated; assigns invited role | `@Valid AcceptInviteRequest` (Token, password complexity enforced) | `200 OK` (JWT) | `AuthenticationSecurityTest`, `InputValidationSecurityTest` |

---

## 2. User & Resident Management Endpoints (`/api/v1/users`)

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users/me` | **Yes** | Any Authenticated | Strictly returns current user's own profile (`currentUser.getId()`) | None | `200 OK` | `AuthenticationSecurityTest` |
| `POST` | `/api/v1/users/change-password` | **Yes** | Any Authenticated | Updates current user's password; verifies old password, evicts user cache | `@Valid ChangePasswordRequest` (Password complexity enforced) | `200 OK` | `SecurityVulnerabilityHarnessTest` |
| `GET` | `/api/v1/users` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Tenant-isolated (`TenantContext.getSocietyId()`); residents receive `403` | Status / Role query params | `200 OK` | `AuthorizationRbacSecurityTest` |
| `PATCH` | `/api/v1/users/{userId}/status` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Cannot modify lateral admins or super-admins; evicts cache on update | `@Valid UpdateStatusRequest` | `200 OK` / `403` | `SecurityVulnerabilityHarnessTest` |
| `PUT` | `/api/v1/users/{userId}/resident` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Prevents admin hijacking of higher/lateral privileges; password complexity enforced | `@Valid UpdateResidentRequest` | `200 OK` / `403` | `AuthorizationRbacSecurityTest`, `SecurityVulnerabilityHarnessTest` |
| `DELETE` | `/api/v1/users/{userId}` | **Yes** | `PLATFORM_ADMIN` | Platform admin only; permanently deletes user and removes from all groups | Valid userId path variable | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |

---

## 3. Maintenance Billing & Payments Endpoints (`/api/v1/finance/bills`)

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/finance/bills` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Scoped to society context; prevents duplicate bill generation per month | `@Valid CreateBillRequest` (Amount positive, YYYY-MM format, future due date) | `201 Created` | `InputValidationSecurityTest` |
| `GET` | `/api/v1/finance/bills` | **Yes** | `ADMIN`, `SUPER_ADMIN`, `ACCOUNTANT`, `RESIDENT` | Admins/staff see society bills; residents see only bills where `userId == currentUser.getId()` | Page, size, status params | `200 OK` | `AuthorizationRbacSecurityTest` |
| `GET` | `/api/v1/finance/bills/{id}` | **Yes** | `ADMIN`, `SUPER_ADMIN`, `ACCOUNTANT`, `RESIDENT` | IDOR Protected: non-staff forbidden if bill userId != currentUser.getId() | Valid bill ID | `200 OK` / `403` | `AuthorizationRbacSecurityTest` |
| `POST` | `/api/v1/finance/bills/{id}/pay` | **Yes** | `RESIDENT` | IDOR Protected: resident must own bill; requires exact full payment amount | `@Valid PayBillRequest` (Transaction ID, Amount positive) | `200 OK` / `403` | `AuthorizationRbacSecurityTest` |
| `POST` | `/api/v1/finance/bills/{id}/verify-payment` | **Yes** | `ADMIN`, `SUPER_ADMIN`, `ACCOUNTANT` | Staff only; approves pending payment reference after bank reconciliation | Valid bill ID | `200 OK` / `403` | `AuthorizationRbacSecurityTest`, `SecurityVulnerabilityHarnessTest` |
| `POST` | `/api/v1/finance/bills/{id}/reject-payment` | **Yes** | `ADMIN`, `SUPER_ADMIN`, `ACCOUNTANT` | Staff only; marks bill payment rejected and records reason | Reason parameter | `200 OK` / `403` | `SecurityVulnerabilityHarnessTest` |

---

## 4. Community Issues & Voting Endpoints (`/api/v1/issues`)

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/issues` | **Yes** | `ADMIN`, `RESIDENT` | Scoped to caller's `societyId`; links creator ID to authenticated principal | `@Valid IssueRequest` (Title, description not blank) | `201 Created` | `InputValidationSecurityTest` |
| `GET` | `/api/v1/issues` | **Yes** | `ADMIN`, `RESIDENT` | Scoped to caller's society; sanitized sorting (`voteCount` or `createdAt` only) | Status, sortBy, direction params | `200 OK` | `InputValidationSecurityTest` |
| `GET` | `/api/v1/issues/{id}` | **Yes** | `ADMIN`, `RESIDENT` | Scoped to caller's society context | Valid issue ID | `200 OK` | `IssueControllerTest` |
| `PATCH` | `/api/v1/issues/{id}/status` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Admin only; residents receive `403 Forbidden` | `@Valid IssueStatusRequest` | `200 OK` / `403` | `AuthorizationRbacSecurityTest` |
| `PATCH` | `/api/v1/issues/{id}/priority` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Admin only; residents receive `403 Forbidden` | `@Valid IssuePriorityRequest` | `200 OK` / `403` | `AuthorizationRbacSecurityTest` |
| `POST` | `/api/v1/issues/{id}/votes` | **Yes** | `ADMIN`, `RESIDENT` | Prevents voting on own issue or duplicate voting | Valid issue ID | `201 Created` / `400` / `409` | `IssueServiceTest` |
| `DELETE` | `/api/v1/issues/{id}/votes` | **Yes** | `ADMIN`, `RESIDENT` | Removes only caller's own vote | Valid issue ID | `204 No Content` | `IssueServiceTest` |

---

## 5. Complaints Management Endpoints (`/api/v1/complaints`)

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/complaints` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN` | Scoped to caller's society; sets residentId to authenticated principal | `@Valid CreateComplaintRequest` | `201 Created` | `InputValidationSecurityTest` |
| `GET` | `/api/v1/complaints` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN` | Staff sees all society complaints; residents see only complaints they filed | Status, category params | `200 OK` | `AuthorizationRbacSecurityTest` |
| `GET` | `/api/v1/complaints/{id}` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN` | IDOR Protected: non-staff forbidden if residentId != currentUser.getId() | Valid complaint ID | `200 OK` / `403` | `AuthorizationRbacSecurityTest` |
| `PATCH` | `/api/v1/complaints/{id}/status` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Admin only; updates complaint workflow status | `@Valid UpdateComplaintRequest` | `200 OK` / `403` | `ComplaintServiceTest` |
| `DELETE` | `/api/v1/complaints/{id}` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN` | IDOR Protected: only OPEN complaints can be deleted by owner | Valid complaint ID | `204 No Content` / `403` | `AuthorizationRbacSecurityTest` |
| `GET` | `/api/v1/complaints/stats` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Aggregated complaint counts; requires active society context (no default tenant) | None | `200 OK` / `403` | `SecurityVulnerabilityHarnessTest` |

---

## 6. Society Registration & Lifecycle Endpoints (`/api/v1/societies`)

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/societies/register` | **No** (Public) | Anyone | Public registration with certificate upload; sets status `PENDING_VERIFICATION` | `@Valid RegisterSocietyRequest`, Multipart max 5MB, PDF/JPEG/PNG allowlist | `202 Accepted` / `400` | `FileUploadSecurityTest`, `RateLimitingSecurityTest` |
| `GET` | `/api/v1/societies/join/{joinCode}` | **No** (Public) | Anyone | Returns public society name and code for onboarding verification | Join code path variable | `200 OK` / `404` | `RateLimitingSecurityTest` |
| `GET` | `/api/v1/societies/me` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN` | Masks `joinCode` and `documentUrl` for residents; exposed only to Admins | None | `200 OK` | `InformationLeakageSecurityTest`, `SecurityVulnerabilityHarnessTest` |
| `PATCH` | `/api/v1/societies/{id}/verify` | **Yes** | `PLATFORM_ADMIN` | Platform Admin only; approves/rejects society registration | `@Valid VerifySocietyRequest` | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |
| `GET` | `/api/v1/societies` | **Yes** | `PLATFORM_ADMIN` | Platform Admin only; lists all registered societies across platform | Status query param | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |
| `GET` | `/api/v1/societies/{id}/document` | **Yes** | `PLATFORM_ADMIN` | Platform Admin only; generates short-lived presigned S3 download URL | Valid society ID | `200 OK` / `403` | `FileUploadSecurityTest`, `PlatformAuthorizationPenetrationTest` |

---

## 7. Community Communication & Groups Endpoints (`/api/v1/groups` & `/api/v1/announcements`)

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/groups/my-group` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN`, `PLATFORM_ADMIN` | Masks member phone numbers and emails for standard residents | None | `200 OK` | `InformationLeakageSecurityTest`, `SecurityVulnerabilityHarnessTest` |
| `GET` | `/api/v1/groups/{groupId}` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN`, `PLATFORM_ADMIN` | Scoped to caller's society; resident accessing another society's group receives `403` | Group ID path variable | `200 OK` / `403` | `AuthorizationRbacSecurityTest` |
| `GET` | `/api/v1/groups` | **Yes** | `PLATFORM_ADMIN` | Platform Admin only; multi-group visibility | None | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |
| `POST` | `/api/v1/groups/{groupId}/members` | **Yes** | `PLATFORM_ADMIN` | Platform Admin only; manual member insertion | `@Valid ManualGroupMemberRequest` | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |
| `DELETE` | `/api/v1/groups/{groupId}/members/{userId}` | **Yes** | `PLATFORM_ADMIN` | Platform Admin only; manual member removal | Group ID, User ID | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |
| `POST` | `/api/v1/announcements` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Admin only; scoped to society context | `@Valid AnnouncementRequest` | `201 Created` / `403` | `AnnouncementControllerTest` |
| `GET` | `/api/v1/announcements` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN` | Scoped to caller's society | Type param | `200 OK` | `AnnouncementControllerTest` |
| `GET` | `/api/v1/announcements/{id}` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN` | Scoped to caller's society | Announcement ID | `200 OK` | `AnnouncementControllerTest` |
| `PUT` | `/api/v1/announcements/{id}` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Admin only; updates announcement content | `@Valid AnnouncementRequest` | `200 OK` / `403` | `AnnouncementControllerTest` |
| `DELETE` | `/api/v1/announcements/{id}` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Admin only; deletes announcement | Announcement ID | `204 No Content` / `403` | `AnnouncementControllerTest` |

---

## 8. Flats & Units Endpoints (`/api/v1/flats`)

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/flats` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Admin only; scoped to society | `@Valid CreateFlatRequest` | `201 Created` / `403` | `FlatServiceTest` |
| `GET` | `/api/v1/flats` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN` | Masks owner phone and email when viewed by standard resident | Block, occupied params | `200 OK` | `FlatServiceTest` |
| `GET` | `/api/v1/flats/{id}` | **Yes** | `RESIDENT`, `ADMIN`, `SUPER_ADMIN` | Masks owner phone and email when viewed by standard resident | Flat ID | `200 OK` | `FlatServiceTest` |
| `PUT` | `/api/v1/flats/{id}` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Admin only; updates flat allocation | `@Valid CreateFlatRequest` | `200 OK` / `403` | `FlatServiceTest` |
| `DELETE` | `/api/v1/flats/{id}` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Admin only; deletes flat record | Flat ID | `204 No Content` / `403` | `FlatServiceTest` |

---

## 9. Platform Governance Endpoints (`/api/v1/platform`)

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/platform/overview` | **Yes** | `PLATFORM_ADMIN` | Platform-wide metrics; strictly rejects society admins and residents | None | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |
| `POST` | `/api/v1/platform/broadcast` | **Yes** | `PLATFORM_ADMIN` | Broadcasts system notice to all tenants | Request body map | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |
| `DELETE` | `/api/v1/platform/users/{userId}`| **Yes** | `PLATFORM_ADMIN` | Permanently deletes user across platform | User ID | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |

---

## 10. Dashboard, Subscriptions & Webhooks Endpoints

| HTTP Method | Route | Auth Required | Permitted Roles | Ownership / Isolation Rule | Input Validation | Expected Status | Security Test Coverage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/dashboard/stats` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Aggregated society metrics; strictly requires active society context | None | `200 OK` / `403` | `DashboardControllerTest` |
| `POST` | `/api/v1/subscriptions/create-order` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Scoped to society subscription plan | `@Valid CreateOrderRequest` | `200 OK` / `403` | `SubscriptionServiceTest` |
| `POST` | `/api/v1/subscriptions/verify-payment` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Verifies Razorpay payment signature before activating society plan | `@Valid VerifyPaymentRequest` | `200 OK` / `403` | `SubscriptionServiceTest` |
| `POST` | `/api/v1/subscriptions/change-plan` | **Yes** | `ADMIN`, `SUPER_ADMIN` | Scoped to society subscription | `@Valid CreateOrderRequest` | `200 OK` / `403` | `SubscriptionServiceTest` |
| `POST` | `/api/v1/webhooks/razorpay` | **No** (Public) | External Gateway | Validates `X-Razorpay-Signature` HMAC-SHA256 signature against secret | Signature header check | `200 OK` / `400` | `WebhookServiceTest` |
| `POST` | `/api/v1/communication/email/test` | **Yes** | `ADMIN`, `SUPER_ADMIN`, `PLATFORM_ADMIN` | Admins restricted to testing their own email address | `@Valid TestEmailRequest` | `200 OK` / `403` | `PlatformAuthorizationPenetrationTest` |
| `GET` | `/api/v1/health` | **No** (Public) | Anyone | Non-sensitive system status indicator | None | `200 OK` | `RateLimitingSecurityTest` |

