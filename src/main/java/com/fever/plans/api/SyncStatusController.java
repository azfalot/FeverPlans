package com.fever.plans.api;

import com.fever.plans.api.dto.SyncStatusResponse;
import com.fever.plans.service.PlanSynchronizationService;
import com.fever.plans.service.SyncStatusTracker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SyncStatusController {
    private final PlanSynchronizationService synchronizationService;
    private final SyncStatusTracker tracker;

    SyncStatusController(
            PlanSynchronizationService synchronizationService,
            SyncStatusTracker tracker) {
        this.synchronizationService = synchronizationService;
        this.tracker = tracker;
    }

    @GetMapping("/internal/sync-status")
    SyncStatusResponse status() {
        return tracker.currentStatus();
    }

    @PostMapping("/internal/sync")
    SyncStatusResponse synchronizeNow() {
        // Reutilizamos el flujo programado para mantener el mismo manejo de solapes y errores.
        synchronizationService.scheduledSync();
        return tracker.currentStatus();
    }
}
