package com.Application.SocietyManagement.subscription.schedular;

import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryScheduler {

    private final SocietyRepository societyRepository;

    @Scheduled(cron = "0 0 2 * * *") // daily at 2 AM
    public void checkExpiries() {
        Instant now = Instant.now();

        List<Society> expiredTrials = societyRepository
                .findBySubscriptionStatusAndTrialEndsAtBefore(SubscriptionStatus.TRIAL, now);
        for (Society society : expiredTrials) {
            society.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
            societyRepository.save(society);
            log.info("Trial expired for society {}", society.getId());
        }

        List<Society> lapsedSubscriptions = societyRepository
                .findBySubscriptionStatusAndSubscriptionEndsAtBefore(SubscriptionStatus.ACTIVE, now);
        for (Society society : lapsedSubscriptions) {
            society.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
            societyRepository.save(society);
            log.info("Subscription lapsed (PAST_DUE) for society {}", society.getId());
        }
    }
}