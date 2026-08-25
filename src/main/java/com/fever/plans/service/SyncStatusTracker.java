package com.fever.plans.service;

import com.fever.plans.api.dto.SyncStatusResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class SyncStatusTracker {
    private final Clock clock;

    /*
     * El scheduler actualiza este estado mientras el endpoint HTTP puede leerlo. Publicar una
     * foto completa e inmutable evita que alguien vea un estado actualizado a medias.
     */
    private final AtomicReference<State> state = new AtomicReference<>(State.empty());

    public SyncStatusTracker(Clock clock) {
        this.clock = clock;
    }

    public void recordSuccess(int processedPlans) {
        var now = clock.instant();
        state.set(new State(now, now, null, processedPlans));
    }

    public void recordFailure(String error) {
        var previous = state.get();
        state.set(new State(clock.instant(), previous.lastSuccessAt(), error, 0));
    }

    public SyncStatusResponse currentStatus() {
        var current = state.get();
        return new SyncStatusResponse(
                current.lastAttemptAt(),
                current.lastSuccessAt(),
                current.lastError(),
                current.lastProcessedPlans());
    }

    private record State(
            Instant lastAttemptAt,
            Instant lastSuccessAt,
            String lastError,
            int lastProcessedPlans) {
        static State empty() {
            return new State(null, null, null, 0);
        }
    }

}
