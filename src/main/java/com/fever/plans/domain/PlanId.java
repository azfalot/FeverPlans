package com.fever.plans.domain;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Provider natural key. The API contract requires UUID output, so this key is
 * deterministically translated into one and remains stable across imports.
 */
public record PlanId(String basePlanId, String providerPlanId) {
    public UUID asUuid() {
        var naturalKey = basePlanId + ":" + providerPlanId;
        return UUID.nameUUIDFromBytes(naturalKey.getBytes(StandardCharsets.UTF_8));
    }
}
