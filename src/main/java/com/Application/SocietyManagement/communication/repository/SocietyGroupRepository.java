package com.Application.SocietyManagement.communication.repository;

import com.Application.SocietyManagement.communication.entity.SocietyGroup;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocietyGroupRepository extends MongoRepository<SocietyGroup, String> {

    Optional<SocietyGroup> findBySocietyId(String societyId);

    boolean existsBySocietyId(String societyId);

    List<SocietyGroup> findByMemberUserIdsContaining(String userId);

    void deleteBySocietyId(String societyId);
}
