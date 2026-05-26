package com.Application.SocietyManagement.Flat.repository;

import com.Application.SocietyManagement.Flat.entity.Flat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FlatRepository extends MongoRepository<Flat, String> {
    Page<Flat> findBySocietyId(String societyId, Pageable pageable);
    Page<Flat> findBySocietyIdAndBlock(
            String societyId, String block, Pageable pageable);
    Page<Flat> findBySocietyIdAndOccupied(
            String societyId, boolean occupied, Pageable pageable);
    Optional<Flat> findByIdAndSocietyId(String id, String societyId);
    boolean existsByFlatNumberAndSocietyId(
            String flatNumber, String societyId);
    List<Flat> findBySocietyIdAndBlock(String societyId, String block);
    long countBySocietyId(String societyId);
    long countBySocietyIdAndOccupied(String societyId, boolean occupied);
}
