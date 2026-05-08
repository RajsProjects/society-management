# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0.x   | ✅        |

## Reporting a Vulnerability

If you discover a security vulnerability, please do NOT open a public GitHub issue.

Instead, please:
1. Email the maintainer directly
2. Include a description of the vulnerability
3. Include steps to reproduce
4. Include potential impact

You will receive a response within 48 hours. If the issue is confirmed, a patch will be released as soon as possible.

## Security Best Practices for Contributors
- Never commit real credentials or API keys
- Always use `application-template.yml` as reference
- Never commit `.env` file
- Always rotate credentials if accidentally exposed
- Use `k8s/secret-template.yaml` for Kubernetes secrets