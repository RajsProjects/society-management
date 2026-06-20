package com.Application.SocietyManagement.society.repository;

import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.enums.SocietyStatus;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SocietyRepository
        extends MongoRepository<Society, String> {

    Optional<Society> findByJoinCode(String joinCode);
    Optional<Society> findByRegistrationNumber(String registrationNumber);
    boolean existsByRegistrationNumber(String registrationNumber);
    boolean existsByAdminEmail(String email);
    Page<Society> findByStatus(SocietyStatus status, Pageable pageable);
    List<Society> findBySubscriptionStatusAndSubscriptionEndsAtBefore(
            SubscriptionStatus status, Instant now);
    List<Society> findBySubscriptionStatusAndTrialEndsAtBefore(
            SubscriptionStatus status, Instant now);
}