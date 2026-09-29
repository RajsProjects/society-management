# Security Changes

Date: 2026-09-24

This file records the security and production-hardening changes made after the
review in `problems.md`. It intentionally contains no credential values.

## Application authorization

- Added tenant-scoped user repository queries and status updates.
- Added tenant-scoped lookups for bills, issues, announcements, and complaints.
- Added complaint authorization checks to status updates.
- Restricted subscription order creation, payment verification, and plan changes
  to society administrators.
- Scoped payment verification orders to the authenticated society.

These changes prevent administrators and residents from reading or modifying
records belonging to another society.

## Payment and configuration security

- Payment signature verification now uses the Razorpay API key secret.
- The webhook secret remains reserved for webhook payload verification.
- Added the missing `security.pepper` configuration to all runtime profiles.
- Added configurable CORS origins instead of allowing every origin.
- Removed trust in client-controlled `X-Forwarded-For` for rate limiting.
- Disabled detailed public actuator health responses.

## Deployment hardening

- Added all required production secret names to the Kubernetes secret template.
- Injected Kubernetes secrets through `envFrom`.
- Added `.env.example` with placeholders and no real credentials.
- Removed public Redis, Prometheus, and Grafana ports from Docker Compose.
- Required explicit Redis and Grafana passwords; removed the default Grafana
  password.
- Added a society-aware unique index for maintenance bills.

## Credential rotation requirement

The previous local `.env` contained live-looking credentials for MongoDB, JWT,
AWS, mail, Razorpay, the platform administrator, and the password pepper.
Those values must be revoked and regenerated in their respective providers.
Changing a local file alone does not revoke credentials that may already have
been copied, logged, or shared.

After provider-side rotation, populate local development values from the
secret manager or an ignored `.env` file based on `.env.example`. Do not commit
`.env`, provider credentials, JWT secrets, passwords, or pepper values.

## Validation performed

- Production package build passed with `.\mvnw.cmd -q -DskipTests package`.
- Focused authentication and JWT tests passed.
- Docker Compose configuration validation passed.
- Full legacy tests still require updates because maintenance-bill unit tests
  mock the previous unscoped repository contract and do not set tenant context.
- Application startup reached MongoDB but failed authentication using the old
  local credentials, confirming that those credentials are not currently
  usable.
