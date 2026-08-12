package com.fever.plans.provider.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Normalized provider plan consumed by the synchronization service. */
public record ProviderPlanData(
        String basePlanId,
        String planId,
        String sellMode,
        String title,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        List<BigDecimal> prices) {
}
