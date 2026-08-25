package com.fever.plans.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyncStatusTrackerTest {
    private final Instant now = Instant.parse("2026-08-24T10:15:30Z");
    private final SyncStatusTracker tracker = new SyncStatusTracker(Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void startsWithEmptyStatusBeforeAnySynchronizationRuns() {
        var status = tracker.currentStatus();

        assertThat(status.last_attempt_at()).isNull();
        assertThat(status.last_success_at()).isNull();
        assertThat(status.last_error()).isNull();
        assertThat(status.last_processed_plans()).isZero();
    }

    @Test
    void recordsSuccessfulSynchronization() {
        tracker.recordSuccess(4);

        var status = tracker.currentStatus();

        assertThat(status.last_attempt_at()).isEqualTo(now);
        assertThat(status.last_success_at()).isEqualTo(now);
        assertThat(status.last_error()).isNull();
        assertThat(status.last_processed_plans()).isEqualTo(4);
    }

    @Test
    void failureKeepsPreviousSuccessfulSynchronization() {
        tracker.recordSuccess(4);
        tracker.recordFailure("Provider returned HTTP 503");

        var status = tracker.currentStatus();

        assertThat(status.last_attempt_at()).isEqualTo(now);
        assertThat(status.last_success_at()).isEqualTo(now);
        assertThat(status.last_error()).isEqualTo("Provider returned HTTP 503");
        assertThat(status.last_processed_plans()).isZero();
    }
}
