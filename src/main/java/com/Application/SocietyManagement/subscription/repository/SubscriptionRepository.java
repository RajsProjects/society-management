package com.Application.SocietyManagement.subscription.repository;

import com.Application.SocietyManagement.subscription.entity.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {
    Optional<Subscription> findByRazorpayOrderId(String razorpayOrderId);
    List<Subscription> findBySocietyIdOrderByCreatedAtDesc(String societyId);
}