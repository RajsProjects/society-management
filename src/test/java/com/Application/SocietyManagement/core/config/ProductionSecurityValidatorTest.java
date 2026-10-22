package com.Application.SocietyManagement.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ProductionSecurityValidatorTest {

    @Mock
    private Environment environment;

    private ProductionSecurityValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new ProductionSecurityValidator(environment);
    }

    @Test
    @DisplayName("Non-production environment permits startup even with default credentials with warnings")
    void nonProductionAllowsDefaults() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        ReflectionTestUtils.setField(validator, "adminPassword", "ChangeMe123!");
        ReflectionTestUtils.setField(validator, "razorpayKeyId", "rzp_test_dummy");

        assertThatCode(() -> validator.validateSecrets()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Production environment throws IllegalStateException if default secrets are used")
    void productionRejectsDefaultSecrets() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        ReflectionTestUtils.setField(validator, "jwtSecret", "replace-with-a-random-secret");
        ReflectionTestUtils.setField(validator, "securityPepper", "replace-with-a-random-secret");
        ReflectionTestUtils.setField(validator, "adminPassword", "ChangeMe123!");
        ReflectionTestUtils.setField(validator, "adminEmail", "admin@civiclink.local");
        ReflectionTestUtils.setField(validator, "razorpayKeyId", "rzp_test_dummy");
        ReflectionTestUtils.setField(validator, "razorpayKeySecret", "dummy");
        ReflectionTestUtils.setField(validator, "razorpayWebhookSecret", "dummy");

        assertThatThrownBy(() -> validator.validateSecrets())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production startup aborted due to");
    }

    @Test
    @DisplayName("Production environment successfully passes when all secrets are properly rotated")
    void productionPassesWhenRotated() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        ReflectionTestUtils.setField(validator, "jwtSecret", "c8f2a937e1b40285d9472304918e65f3a0981726453891024756182930491827");
        ReflectionTestUtils.setField(validator, "securityPepper", "9f8e7d6c5b4a3210123456789abcdef0");
        ReflectionTestUtils.setField(validator, "adminPassword", "SuperSecureP@ssw0rd2026!");
        ReflectionTestUtils.setField(validator, "adminEmail", "ops@samaaj.in");
        ReflectionTestUtils.setField(validator, "razorpayKeyId", "rzp_live_abc123xyz456");
        ReflectionTestUtils.setField(validator, "razorpayKeySecret", "secret_live_key_98765");
        ReflectionTestUtils.setField(validator, "razorpayWebhookSecret", "whsec_live_token_4321");

        assertThatCode(() -> validator.validateSecrets()).doesNotThrowAnyException();
    }
}
