package com.fever.plans.api.dto;

import java.time.Instant;

public record SyncStatusResponse(
        Instant last_attempt_at,
        Instant last_success_at,
        String last_error,
        int last_processed_plans) {
}