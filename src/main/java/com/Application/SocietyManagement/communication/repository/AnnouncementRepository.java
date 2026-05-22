package com.Application.SocietyManagement.communication.repository;

import com.Application.SocietyManagement.communication.entity.Announcement;
import com.Application.SocietyManagement.communication.enums.AnnouncementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AnnouncementRepository extends MongoRepository<Announcement, String> {
    Page<Announcement> findByType(AnnouncementType type, Pageable pageable);
    Page<Announcement> findBySocietyId(String societyId, Pageable pageable);
    Page<Announcement> findByTypeAndSocietyId(AnnouncementType type, String societyId, Pageable pageable);

}
