package com.Application.SocietyManagement.users.repository;

import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByRole(Roles roles);
    Page<User> findByStatus(Status status, Pageable pageable);
    Page<User> findByRole(Roles role, Pageable pageable);
    Page<User> findByStatusAndRole(Status status, Roles role, Pageable pageable);
}