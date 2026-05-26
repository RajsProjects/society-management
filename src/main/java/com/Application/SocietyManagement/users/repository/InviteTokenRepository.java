package com.Application.SocietyManagement.users.repository;

import com.Application.SocietyManagement.users.entity.InviteToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InviteTokenRepository
        extends MongoRepository<InviteToken, String> {
    Optional<InviteToken> findByToken(String token);
    boolean existsByEmailAndSocietyIdAndUsedFalse(
            String email, String societyId);
    void deleteByExpiresAtBefore(Instant now);
    List<InviteToken> findBySocietyId(String societyId);
}
