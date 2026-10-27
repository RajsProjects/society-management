package com.Application.SocietyManagement.core.security;

import com.Application.SocietyManagement.complaint.dto.CreateComplaintRequest;
import com.Application.SocietyManagement.finance.dto.CreateBillRequest;
import com.Application.SocietyManagement.users.dto.AcceptInviteRequest;
import com.Application.SocietyManagement.users.dto.ChangePasswordRequest;
import com.Application.SocietyManagement.users.dto.SignupRequest;
import com.Application.SocietyManagement.users.dto.UpdateResidentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Security: Input Validation, Schema Enforcement & Injection Defense Test Suite")
class InputValidationSecurityTest {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Input Validation: SignupRequest rejects missing mandatory fields")
    void signupRequest_missingFields_violatesConstraints() {
        SignupRequest request = new SignupRequest();
        // email, password, firstName, lastName null or blank

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("email", "password", "firstName", "lastName");
    }

    @Test
    @DisplayName("Input Validation: SignupRequest rejects malformed email addresses")
    void signupRequest_malformedEmail_violatesConstraints() {
        SignupRequest request = new SignupRequest();
        request.setEmail("not-a-valid-email-format");
        request.setPassword("StrongP@ss1");
        request.setFirstName("Valid");
        request.setLastName("User");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("email");
    }

    @Test
    @DisplayName("Password Security: SignupRequest rejects passwords failing complexity requirements")
    void signupRequest_weakPassword_violatesConstraints() {
        // Missing uppercase and numbers, shorter than 8 chars
        SignupRequest request = new SignupRequest();
        request.setEmail("valid@test.com");
        request.setPassword("weak");
        request.setFirstName("Valid");
        request.setLastName("User");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("password");
    }

    @Test
    @DisplayName("Password Security: UpdateResidentRequest rejects weak newPassword when specified")
    void updateResidentRequest_weakNewPassword_violatesConstraints() {
        UpdateResidentRequest request = UpdateResidentRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .newPassword("simple123") // Missing uppercase letter
                .build();

        Set<ConstraintViolation<UpdateResidentRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("newPassword");
    }

    @Test
    @DisplayName("Password Security: AcceptInviteRequest enforces strong password pattern")
    void acceptInviteRequest_weakPassword_violatesConstraints() {
        AcceptInviteRequest request = new AcceptInviteRequest();
        request.setToken("invite-token-abc");
        request.setPassword("no_numbers_nor_caps");
        request.setFirstName("Alice");
        request.setLastName("Smith");

        Set<ConstraintViolation<AcceptInviteRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("password");
    }

    @Test
    @DisplayName("Numeric Validation: CreateBillRequest rejects negative or zero bill amounts")
    void createBillRequest_negativeAmount_violatesConstraints() {
        CreateBillRequest request = new CreateBillRequest();
        request.setUserId("user-1");
        request.setApartmentNumber("A-101");
        request.setBillingMonth("2026-10");
        request.setDueDate(java.time.LocalDate.now().plusDays(10));
        request.setAmount(BigDecimal.valueOf(-500.00)); // Negative amount

        Set<ConstraintViolation<CreateBillRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("amount");
    }

    @Test
    @DisplayName("Text Sanitization: CreateComplaintRequest rejects blank title or description")
    void createComplaintRequest_blankText_violatesConstraints() {
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("   ");
        request.setDescription("");
        request.setCategory(com.Application.SocietyManagement.complaint.enums.ComplaintCategory.OTHER);

        Set<ConstraintViolation<CreateComplaintRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("title", "description");
    }

    @Test
    @DisplayName("NoSQL Injection Defense: Special MongoDB operators in email are treated as literal strings")
    void nosqlInjectionOperators_treatedAsLiteralStrings() {
        SignupRequest request = new SignupRequest();
        // Attacker attempts Mongo query injection in email field
        request.setEmail("{\"$ne\": null}@test.com");
        request.setPassword("ValidP@ssw0rd1");
        request.setFirstName("John");
        request.setLastName("Doe");

        // Jakarta email validation strictly rejects special curly brackets or injection operators
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .contains("email");
    }
}

