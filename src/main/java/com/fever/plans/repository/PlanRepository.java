package com.fever.plans.repository;

import com.fever.plans.domain.Plan;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    Optional<Plan> findByBasePlanIdAndProviderPlanId(String basePlanId, String providerPlanId);

    List<Plan> findAllByOrderByStartsAtAsc();

    List<Plan> findByStartsAtGreaterThanOrderByStartsAtAsc(LocalDateTime startsAt);

    List<Plan> findByEndsAtLessThanOrderByStartsAtAsc(LocalDateTime endsAt);

    List<Plan> findByStartsAtGreaterThanAndEndsAtLessThanOrderByStartsAtAsc(
            LocalDateTime startsAt,
            LocalDateTime endsAt);
}
