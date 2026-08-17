package com.fever.plans.service;

import com.fever.plans.provider.PlanProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
    private final PlanImportService importService;

    public PlanSynchronizationService(PlanProvider provider, PlanImportService importService) {
        this.provider = provider;
        this.importService = importService;
    }

    @Scheduled(
            initialDelayString = "${provider.initial-delay:PT1S}",
            fixedDelayString = "${provider.sync-delay}")
    public void scheduledSync() {
        try {
            sync();
        } catch (RuntimeException exception) {
            log.warn(
                    "Provider synchronization failed ({}); local search data is unchanged",
                    exception.getMessage());
        }
    }

    public void sync() {
        for (var plan : provider.fetchPlans()) {
            try {
                importService.importPlan(plan);
            } catch (DataIntegrityViolationException exception) {
                log.warn(
                        "Skipping provider plan {}/{} because it violates persistence constraints",
                        plan.basePlanId(),
                        plan.planId());
            }
        }
    }
}
