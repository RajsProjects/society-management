# Security Penetration Testing Runbook

This runbook provides the operational instructions for executing, troubleshooting, and extending the automated security test suite and penetration test harness for the CivicLink Society Management backend.

---

## 1. Safety Boundary & Scope

- **Authorized Environment:** All testing is strictly scoped to this repository (`SocietyManagement`) and local/isolated unit test environments using Spring Boot Test doubles, MockMvc, and Mockito.
- **Data Protection:** Tests run in-memory and do not connect to external production MongoDB clusters, AWS S3 buckets, or live Redis instances.
- **Zero Real Credentials:** All test keys, passwords, and tokens are ephemeral synthetic fixtures. No real credentials or user secrets are present in code or test logs.

---

## 2. Test Suite Architecture

The security test suite is located in the test tree:
`src/test/java/com/Application/SocietyManagement/core/security/`

| Test Class | Focus Area | Test Count | Key Invariants Verified |
| :--- | :--- | :--- | :--- |
| [`SecurityVulnerabilityHarnessTest.java`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/src/test/java/com/Application/SocietyManagement/core/security/SecurityVulnerabilityHarnessTest.java) | Ralph Loop Regression Harness | 11 | Complete verification of all 9 remediated security flaws |
| [`AuthenticationSecurityTest.java`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/src/test/java/com/Application/SocietyManagement/core/security/AuthenticationSecurityTest.java) | Authentication & Account Security | 12 | Token presence, tampering, expiry, status gating (`PENDING`/`INACTIVE`/`BLOCKED`) |
| [`AuthorizationRbacSecurityTest.java`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/src/test/java/com/Application/SocietyManagement/core/security/AuthorizationRbacSecurityTest.java) | RBAC, IDOR & Multi-Tenant Isolation | 9 | Cross-resident IDOR, tenant barrier, privilege escalation prevention |
| [`JwtSecurityTest.java`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/src/test/java/com/Application/SocietyManagement/core/security/JwtSecurityTest.java) | JWT Cryptography & Claims | 6 | HMAC-SHA signature enforcement, secret length validation (>=256 bits), claim leakage |
| [`FileUploadSecurityTest.java`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/src/test/java/com/Application/SocietyManagement/core/security/FileUploadSecurityTest.java) | File Upload, MIME & S3 Storage | 5 | 5MB size limit, executable MIME block, ZIP rejection, UUID path traversal defense |
| [`RateLimitingSecurityTest.java`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/src/test/java/com/Application/SocietyManagement/core/security/RateLimitingSecurityTest.java) | Abuse Resistance & Throttling | 6 | 120 req/min window, IP extraction without `X-Forwarded-For` spoofing, Redis fail-open |
| [`InputValidationSecurityTest.java`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/src/test/java/com/Application/SocietyManagement/core/security/InputValidationSecurityTest.java) | Schema Enforcement & Injection | 8 | Jakarta Bean Validation, password regex patterns, NoSQL operator neutralization |
| [`InformationLeakageSecurityTest.java`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/src/test/java/com/Application/SocietyManagement/core/security/InformationLeakageSecurityTest.java) | Cryptography & Data Masking | 6 | Peppered BCrypt, DTO password exclusion, CORS origin restriction, PII masking |
| [`PlatformAuthorizationPenetrationTest.java`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/src/test/java/com/Application/SocietyManagement/core/security/PlatformAuthorizationPenetrationTest.java) | Platform Admin Governance | 4 | Platform-level operations restricted exclusively to `PLATFORM_ADMIN` |

---

## 3. Prerequisites & Environment Setup

- **Java Development Kit:** JDK 21+ (`openjdk 21.0.x` or higher)
- **Build Tool:** Apache Maven 3.9+
- **Command Line:** PowerShell, Bash, or Zsh

Ensure your environment has `JAVA_HOME` pointing to JDK 21:
```powershell
java -version
mvn -version
```

---

## 4. Execution Commands

### A. Run Entire Security Penetration Test Suite (67 Tests)
To run all security-focused test classes in isolation:
```powershell
mvn test -Dtest="*SecurityTest*,*HarnessTest*,*PenetrationTest*" -f SocietyManagement/pom.xml
```
*Expected Output:* `Tests run: 67, Failures: 0, Errors: 0, Skipped: 0` $\rightarrow$ `BUILD SUCCESS`.

### B. Run Individual Security Test Classes

```powershell
# 1. Run the Ralph Loop Regression Harness (Mitigated Vulnerabilities)
mvn test -Dtest=SecurityVulnerabilityHarnessTest -f SocietyManagement/pom.xml

# 2. Run Authentication & Account Security Tests
mvn test -Dtest=AuthenticationSecurityTest -f SocietyManagement/pom.xml

# 3. Run Authorization, RBAC & IDOR Tests
mvn test -Dtest=AuthorizationRbacSecurityTest -f SocietyManagement/pom.xml

# 4. Run JWT Integrity Tests
mvn test -Dtest=JwtSecurityTest -f SocietyManagement/pom.xml

# 5. Run File Upload & Storage Security Tests
mvn test -Dtest=FileUploadSecurityTest -f SocietyManagement/pom.xml

# 6. Run Rate Limiting & Abuse Resistance Tests
mvn test -Dtest=RateLimitingSecurityTest -f SocietyManagement/pom.xml

# 7. Run Input Validation & Injection Tests
mvn test -Dtest=InputValidationSecurityTest -f SocietyManagement/pom.xml

# 8. Run Information Leakage & Cryptography Tests
mvn test -Dtest=InformationLeakageSecurityTest -f SocietyManagement/pom.xml

# 9. Run Platform Governance Penetration Tests
mvn test -Dtest=PlatformAuthorizationPenetrationTest -f SocietyManagement/pom.xml
```

### C. Run Full Project Regression Suite (304 Tests)
To run all application unit tests, service tests, controller tests, and security tests:
```powershell
mvn test -f SocietyManagement/pom.xml
```
*Expected Output:* `Tests run: 304, Failures: 0, Errors: 0, Skipped: 0` $\rightarrow$ `BUILD SUCCESS`.

---

## 5. Troubleshooting & Common Scenarios

### Warning: `A Java agent has been loaded dynamically (ByteBuddyAgent)`
- **Symptom:** OpenJDK prints a warning that a Java agent was loaded dynamically for Mockito's inline mock maker.
- **Explanation:** This is standard JDK 21 behavior when libraries self-attach agents at runtime. It does not affect test execution.
- **Suppression (Optional):** Add `-Djdk.instrument.traceUsage -XX:+EnableDynamicAgentLoading` to `MAVEN_OPTS`.

### TenantContext Leakage Across Tests
- **Symptom:** Subsequent tests throw `403 Access denied` or report unexpected society ID.
- **Solution:** Every test class that invokes `TenantContext.setSocietyId(...)` includes an `@AfterEach` teardown method calling `TenantContext.clear()`. Always verify this pattern when authoring new tests.

### MockHttpServletRequest in Security Configuration Tests
- **Symptom:** `NullPointerException` inside Spring's `UrlPathHelper.decodeInternal`.
- **Solution:** Do not use raw Mockito mocks for `HttpServletRequest` when invoking `CorsConfigurationSource` or `SecurityFilterChain`. Use Spring's `MockHttpServletRequest("GET", "/api/v1/health")`.

---

## 6. Extending the Test Suite

When adding new endpoints to the application:
1. Update [`ENDPOINT_SECURITY_MATRIX.md`](file:///D:/MyFolder/projects/SocietyManagement/SocietyManagement/docs/security/ENDPOINT_SECURITY_MATRIX.md) with the new route, roles, and ownership rules.
2. Add corresponding negative authorization and validation test cases to the appropriate security test class.
3. Verify that non-admin callers receive `403 FORBIDDEN` when accessing administrative operations.
4. Execute `mvn test -f SocietyManagement/pom.xml` to verify 100% pass and 0 regressions before committing.

