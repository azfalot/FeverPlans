package com.fever.plans.api;

import com.fever.plans.api.dto.SyncStatusResponse;
import com.fever.plans.service.SyncStatusTracker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SyncStatusController {
    private final SyncStatusTracker tracker;

    SyncStatusController(SyncStatusTracker tracker) {
        this.tracker = tracker;
    }

    @GetMapping("/internal/sync-status")
    SyncStatusResponse status() {
        return tracker.currentStatus();
    }
}
