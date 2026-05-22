package com.Application.SocietyManagement.society.repository;

import com.Application.SocietyManagement.society.entity.Society;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SocietyRepository extends MongoRepository<Society, String> {
    Optional<Society> findBySocietyCode(String societyCode);
    boolean existsBySocietyCode(String societyCode);
    boolean existsByAdminEmail(String adminEmail);
}