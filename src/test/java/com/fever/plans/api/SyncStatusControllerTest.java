package com.fever.plans.api;

import com.fever.plans.api.dto.SyncStatusResponse;
import com.fever.plans.service.PlanSynchronizationService;
import com.fever.plans.service.SyncStatusTracker;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SyncStatusController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                ServletWebSecurityAutoConfiguration.class
        }
)
class SyncStatusControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    SyncStatusTracker tracker;

    @MockitoBean
    PlanSynchronizationService synchronizationService;

    @Test
    void returnsTheCurrentSynchronizationStatus() throws Exception {
        when(tracker.currentStatus()).thenReturn(new SyncStatusResponse(
                Instant.parse("2026-08-24T10:15:30Z"),
                Instant.parse("2026-08-24T10:15:30Z"),
                null,
                4));

        mvc.perform(get("/internal/sync-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last_attempt_at").value("2026-08-24T10:15:30Z"))
                .andExpect(jsonPath("$.last_success_at").value("2026-08-24T10:15:30Z"))
                .andExpect(jsonPath("$.last_error").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.last_processed_plans").value(4));
    }

    @Test
    void triggersSynchronizationAndReturnsTheUpdatedStatus() throws Exception {
        when(tracker.currentStatus()).thenReturn(new SyncStatusResponse(
                Instant.parse("2026-08-26T08:00:00Z"),
                Instant.parse("2026-08-26T08:00:00Z"),
                null,
                7));

        mvc.perform(post("/internal/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last_attempt_at").value("2026-08-26T08:00:00Z"))
                .andExpect(jsonPath("$.last_success_at").value("2026-08-26T08:00:00Z"))
                .andExpect(jsonPath("$.last_processed_plans").value(7));

        org.mockito.Mockito.verify(synchronizationService).scheduledSync();
    }
}
