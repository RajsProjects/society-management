package com.Application.SocietyManagement.core.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates critical security secrets at startup.
 * In production profiles ("prod" or "production"), it strictly enforces rotated,
 * cryptographically strong secrets and prevents startup if default/test credentials are used.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionSecurityValidator {

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${security.pepper:}")
    private String securityPepper;

    @Value("${platform.admin.password:}")
    private String adminPassword;

    @Value("${platform.admin.email:}")
    private String adminEmail;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @PostConstruct
    public void validateSecrets() {
        boolean isProduction = environment.acceptsProfiles(Profiles.of("prod", "production"));

        if (!isProduction) {
            // Development sanity warnings
            if ("ChangeMe123!".equals(adminPassword)) {
                log.warn("SECURITY WARNING: Using default PLATFORM_ADMIN_PASSWORD ('ChangeMe123!'). Rotate immediately before staging/production!");
            }
            if (razorpayKeyId != null && razorpayKeyId.contains("dummy")) {
                log.info("Running with Razorpay sandbox/dummy test credentials in non-production profile.");
            }
            return;
        }

        log.info("Enforcing production security validations and secret integrity checks...");
        List<String> violations = new ArrayList<>();

        // 1. JWT Secret Verification
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.contains("replace-with") || jwtSecret.length() < 32) {
            violations.add("JWT_SECRET must be set in production, at least 32 characters (256-bit entropy), and not contain default placeholders.");
        }

        // 2. Security Pepper Verification
        if (securityPepper == null || securityPepper.isBlank() || securityPepper.contains("replace-with") || securityPepper.length() < 16) {
            violations.add("SECURITY_PEPPER must be set in production, at least 16 characters, and not contain default placeholders.");
        }

        // 3. Platform Admin Password Verification
        if (adminPassword == null || adminPassword.isBlank() || "ChangeMe123!".equals(adminPassword) || adminPassword.length() < 12) {
            violations.add("PLATFORM_ADMIN_PASSWORD must be explicitly rotated in production (min 12 chars, not 'ChangeMe123!').");
        }

        // 4. Platform Admin Email Verification
        if (adminEmail == null || adminEmail.isBlank() || adminEmail.contains("local") || adminEmail.contains("example")) {
            violations.add("PLATFORM_ADMIN_EMAIL must be a valid production operational address.");
        }

        // 5. Razorpay Credentials Verification
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeyId.startsWith("rzp_test_") || "dummy".equalsIgnoreCase(razorpayKeyId)) {
            violations.add("RAZORPAY_KEY_ID must be a live production key ('rzp_live_...') in production profile.");
        }
        if (razorpayKeySecret == null || razorpayKeySecret.isBlank() || "dummy".equalsIgnoreCase(razorpayKeySecret)) {
            violations.add("RAZORPAY_KEY_SECRET must be set to a valid production secret in production profile.");
        }
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank() || "dummy".equalsIgnoreCase(razorpayWebhookSecret)) {
            violations.add("RAZORPAY_WEBHOOK_SECRET must be configured for webhook signature validation in production.");
        }

        if (!violations.isEmpty()) {
            log.error("==========================================================================");
            log.error(" CRITICAL PRODUCTION SECURITY AUDIT FAILURE - REFUSING TO START TOMCAT");
            log.error("==========================================================================");
            violations.forEach(v -> log.error(" -> [SECURITY VIOLATION] {}", v));
            log.error("==========================================================================");
            throw new IllegalStateException("Production startup aborted due to " + violations.size()
                    + " unrotated or insecure secrets. Please provide real production secrets via environment variables.");
        }

        log.info("Production security secrets verified: All required secrets are rotated and hardened.");
    }
}
