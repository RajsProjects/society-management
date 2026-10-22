#!/usr/bin/env bash
# ==============================================================================
# Production Secrets Generation Script for Samaaj / CivicLink
# Generates cryptographically secure, high-entropy secrets for production rollout.
# ==============================================================================

set -euo pipefail

echo "=========================================================================="
echo "🔐 SAMAAJ / CIVICLINK PRODUCTION SECRETS GENERATOR"
echo "=========================================================================="

JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
SECURITY_PEPPER=$(openssl rand -hex 32)
PLATFORM_ADMIN_PASSWORD=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9!@#$%^&*' | head -c 20)
REDIS_PASSWORD=$(openssl rand -hex 24)
GRAFANA_PASSWORD=$(openssl rand -base64 18 | tr -dc 'A-Za-z0-9!@#$%^&*' | head -c 16)

echo ""
echo "Generated High-Entropy Secrets:"
echo "--------------------------------------------------------------------------"
echo "JWT_SECRET=${JWT_SECRET}"
echo "SECURITY_PEPPER=${SECURITY_PEPPER}"
echo "PLATFORM_ADMIN_PASSWORD=${PLATFORM_ADMIN_PASSWORD}"
echo "REDIS_PASSWORD=${REDIS_PASSWORD}"
echo "GRAFANA_ADMIN_PASSWORD=${GRAFANA_PASSWORD}"
echo "--------------------------------------------------------------------------"
echo "⚠️  Store these secrets securely in your Production Vault, AWS Secrets Manager,"
echo "   or CI/CD Environment Variables. Never commit them to version control!"
echo "=========================================================================="
