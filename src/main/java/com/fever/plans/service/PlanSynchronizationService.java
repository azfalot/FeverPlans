package com.fever.plans.service;

import com.fever.plans.domain.Plan;
import com.fever.plans.domain.PlanId;
import com.fever.plans.repository.PlanRepository;
import com.fever.plans.provider.PlanProvider;
import com.fever.plans.provider.dto.ProviderPlanData;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final SyncStatusTracker syncStatusTracker;
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    public PlanSynchronizationService(
            PlanProvider provider,
            PlanRepository repository,
            SyncStatusTracker syncStatusTracker) {
        this.provider = provider;
        this.repository = repository;
        this.syncStatusTracker = syncStatusTracker;
    }

    @Scheduled(initialDelayString = "PT1S", fixedDelayString = "${provider.sync-delay}")
    public void scheduledSync() {
        // Candado local: evita dos sincronizaciones solapadas dentro de la misma instancia.
        if (!syncRunning.compareAndSet(false, true)) {
            log.info("Skipping provider synchronization because another one is still running");
            return;
        }

        try {
            sync();
        } catch (RuntimeException exception) {
            syncStatusTracker.recordFailure(exception.getMessage());
            log.warn(
                    "Provider synchronization failed ({}); local search data is unchanged",
                    exception.getMessage());
        } finally {
            syncRunning.set(false);
        }
    }

    @Transactional
    public void sync() {
        var plansToSynchronize = provider.fetchPlans().stream()
                .filter(this::shouldSynchronize)
                .toList();

        if (plansToSynchronize.isEmpty()) {
            syncStatusTracker.recordSuccess(0);
            return;
        }

        var existingPlansByProviderKey = findExistingPlansByProviderKey(plansToSynchronize);
        plansToSynchronize.forEach(plan -> synchronizeOnlinePlan(
                plan,
                existingPlansByProviderKey.get(providerKey(plan.basePlanId(), plan.planId()))));

        syncStatusTracker.recordSuccess(plansToSynchronize.size());
    }

    private boolean shouldSynchronize(ProviderPlanData plan) {
        if (!"online".equalsIgnoreCase(plan.sellMode())) {
            return false;
        }
        if (!hasRequiredFields(plan)) {
            log.warn("Skipping provider plan with missing identifiers or dates");
            return false;
        }
        return true;
    }

    private Map<String, Plan> findExistingPlansByProviderKey(List<ProviderPlanData> plansToSynchronize) {
        var basePlanIds = plansToSynchronize.stream()
                .map(ProviderPlanData::basePlanId)
                .collect(Collectors.toSet());

        // Carga en lote: una query por snapshot en vez de una query por cada plan.
        return repository.findByBasePlanIdIn(basePlanIds).stream()
                .collect(Collectors.toMap(
                        plan -> providerKey(plan.getBasePlanId(), plan.getProviderPlanId()),
                        Function.identity()));
    }

    private void synchronizeOnlinePlan(ProviderPlanData plan, Plan existingPlan) {
        var prices = plan.prices() == null ? List.<BigDecimal>of() : plan.prices();
        var minPrice = prices.stream().min(Comparator.naturalOrder()).orElse(null);
        var maxPrice = prices.stream().max(Comparator.naturalOrder()).orElse(null);

        if (existingPlan != null) {
            existingPlan.update(plan.title(), plan.startsAt(), plan.endsAt(), minPrice, maxPrice);
            return;
        }

        repository.save(new Plan(
                new PlanId(plan.basePlanId(), plan.planId()),
                plan.title(),
                plan.startsAt(),
                plan.endsAt(),
                minPrice,
                maxPrice));
    }

    private boolean hasRequiredFields(ProviderPlanData plan) {
        return plan.basePlanId() != null
                && plan.planId() != null
                && plan.startsAt() != null
                && plan.endsAt() != null;
    }

    private String providerKey(String basePlanId, String providerPlanId) {
        return basePlanId + ":" + providerPlanId;
    }

}
