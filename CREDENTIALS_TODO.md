# Credentials and Secrets TODO

Use this checklist before deploying the application. Never write actual secret
values in this file, source code, Git commits, issue trackers, or chat messages.

## Immediate rotation

- [ ] Rotate the MongoDB database user password and update the connection URI.
- [ ] Rotate the JWT signing secret.
- [ ] Rotate the security pepper.
- [ ] Rotate AWS access keys or replace them with an IAM role/workload identity.
- [ ] Rotate the Razorpay API key secret.
- [ ] Rotate the Razorpay webhook secret.
- [ ] Rotate the SMTP/mail app password.
- [ ] Change the platform administrator password.
- [ ] Rotate the Redis password.
- [ ] Set a unique Grafana administrator password.

## Required runtime variables

### Database and authentication

- [ ] `MONGODB_URI` — MongoDB connection URI with the production database.
- [ ] `JWT_SECRET` — high-entropy signing key; use at least 32 random bytes.
- [ ] `JWT_EXPIRATION` — token lifetime in milliseconds.
- [ ] `SECURITY_PEPPER` — high-entropy password pepper kept outside the database.

### Mail

- [ ] `MAIL_USERNAME` — SMTP account used for notifications.
- [ ] `MAIL_APP_PASSWORD` — SMTP app-specific password.

### AWS S3

- [ ] `AWS_ACCESS_KEY_ID` — only if workload identity/IAM role is unavailable.
- [ ] `AWS_SECRET_ACCESS_KEY` — only if workload identity/IAM role is unavailable.
- [ ] `AWS_REGION` — AWS region containing the S3 bucket.
- [ ] Confirm the S3 bucket name and least-privilege IAM permissions.
- [ ] Confirm the bucket blocks public access and uses encryption.

### Razorpay

- [ ] `RAZORPAY_KEY_ID` — production Razorpay key ID.
- [ ] `RAZORPAY_KEY_SECRET` — production API key secret used for payment verification.
- [ ] `RAZORPAY_WEBHOOK_SECRET` — separate webhook signing secret.
- [ ] Configure Razorpay webhook URL and event subscriptions.
- [ ] Verify that test credentials are not used in production.

### Platform administration

- [ ] `PLATFORM_ADMIN_EMAIL` — initial platform administrator email.
- [ ] `PLATFORM_ADMIN_PASSWORD` — strong one-time bootstrap password.
- [ ] Change the bootstrap administrator password after first login.
- [ ] Confirm platform admin access is protected with MFA where supported.

### Infrastructure

- [ ] `REDIS_HOST` — internal Redis hostname or service name.
- [ ] `REDIS_PORT` — internal Redis port.
- [ ] `REDIS_PASSWORD` — strong Redis password or ACL configuration.
- [ ] `GRAFANA_ADMIN_PASSWORD` — strong Grafana administrator password.
- [ ] `CORS_ALLOWED_ORIGINS` — comma-separated list of trusted frontend origins.

## Secret storage and deployment

- [ ] Store production values in a managed secret store or Kubernetes Secret.
- [ ] Do not commit `.env`; use `.env.example` only as a variable reference.
- [ ] Ensure Kubernetes secrets are injected through `envFrom` or explicit
      `secretKeyRef` entries.
- [ ] Restrict secret read permissions to the application deployment identity.
- [ ] Enable secret audit logging and expiration/rotation reminders.
- [ ] Ensure CI logs mask all secret values.
- [ ] Confirm backups and artifact stores do not contain old `.env` files.
- [ ] Remove or revoke any credentials that were previously shared or exposed.

## Verification after rotation

- [ ] Start the application with the new values.
- [ ] Check MongoDB connectivity and authentication.
- [ ] Check Redis connectivity and rate limiting.
- [ ] Send a test notification using the SMTP provider.
- [ ] Upload and retrieve a test S3 document using a presigned URL.
- [ ] Create and verify a Razorpay test payment in the correct environment.
- [ ] Send a signed Razorpay webhook and confirm it is accepted.
- [ ] Confirm invalid JWTs and expired JWTs are rejected.
- [ ] Confirm actuator details are not publicly exposed.
- [ ] Confirm Redis, Prometheus, and Grafana are not publicly reachable.
- [ ] Remove temporary test credentials and test data after verification.
