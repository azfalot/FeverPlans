package com.fever.plans.service;

import com.fever.plans.domain.Plan;
import com.fever.plans.domain.PlanId;
import com.fever.plans.repository.PlanRepository;
import com.fever.plans.provider.PlanProvider;
import com.fever.plans.provider.dto.ProviderPlanData;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports provider snapshots without deleting previously eligible plans.
 *
 * <p>The provider is deliberately called only from this background path: API searches read the
 * local database and remain available while a synchronization fails.</p>
 */
@Service
public class PlanSynchronizationService {
    private static final Logger log = LoggerFactory.getLogger(PlanSynchronizationService.class);

    private final PlanProvider provider;
    private final PlanRepository repository;

    public PlanSynchronizationService(PlanProvider provider, PlanRepository repository) {
        this.provider = provider;
        this.repository = repository;
    }

    @Scheduled(initialDelayString = "PT1S", fixedDelayString = "${provider.sync-delay}")
    public void scheduledSync() {
        try {
            sync();
        } catch (RuntimeException exception) {
            log.warn(
                    "Provider synchronization failed ({}); local search data is unchanged",
                    exception.getMessage());
        }
    }

    @Transactional
    public void sync() {
        for (var plan : provider.fetchPlans()) {
            synchronizeOnlinePlan(plan);
        }
    }

    private void synchronizeOnlinePlan(ProviderPlanData plan) {
        if (!"online".equalsIgnoreCase(plan.sellMode())) {
            return;
        }
        if (!hasRequiredFields(plan)) {
            log.warn("Skipping provider plan with missing identifiers or dates");
            return;
        }

        var prices = plan.prices() == null ? List.<BigDecimal>of() : plan.prices();
        var minPrice = prices.stream().min(Comparator.naturalOrder()).orElse(null);
        var maxPrice = prices.stream().max(Comparator.naturalOrder()).orElse(null);

        repository.findByBasePlanIdAndProviderPlanId(plan.basePlanId(), plan.planId())
                .ifPresentOrElse(
                        existingPlan -> existingPlan.update(
                                plan.title(), plan.startsAt(), plan.endsAt(), minPrice, maxPrice),
                        () -> repository.save(new Plan(
                                new PlanId(plan.basePlanId(), plan.planId()),
                                plan.title(),
                                plan.startsAt(),
                                plan.endsAt(),
                                minPrice,
                                maxPrice)));
    }

    private boolean hasRequiredFields(ProviderPlanData plan) {
        return plan.basePlanId() != null
                && plan.planId() != null
                && plan.startsAt() != null
                && plan.endsAt() != null;
    }

}
