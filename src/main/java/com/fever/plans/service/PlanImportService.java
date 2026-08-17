package com.fever.plans.service;

import com.fever.plans.domain.Plan;
import com.fever.plans.domain.PlanId;
import com.fever.plans.provider.dto.ProviderPlanData;
import com.fever.plans.repository.PlanRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists individual provider observations in isolated database transactions. */
@Service
public class PlanImportService {
    private static final Logger log = LoggerFactory.getLogger(PlanImportService.class);

    private final PlanRepository repository;

    public PlanImportService(PlanRepository repository) {
        this.repository = repository;
    }

    /**
     * Imports one eligible provider plan atomically.
     *
     * <p>The provider request and XML parsing happen before this boundary, so a slow provider does
     * not keep a database transaction open. Invocation from {@link PlanSynchronizationService}
     * crosses a Spring bean boundary, ensuring that transactional interception is applied. A data
     * error rolls back only this plan, allowing the synchronization coordinator to retain other
     * valid observations from the same provider response.</p>
     */
    @Transactional
    public void importPlan(ProviderPlanData plan) {
        if (!"online".equalsIgnoreCase(plan.sellMode())) {
            return;
        }
        if (!hasRequiredFields(plan)) {
            log.warn("Skipping provider plan with missing identifiers, title or dates");
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
                && plan.title() != null
                && plan.startsAt() != null
                && plan.endsAt() != null;
    }
}
