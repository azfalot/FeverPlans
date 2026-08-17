package com.fever.plans.service;

import com.fever.plans.provider.PlanProvider;
import com.fever.plans.provider.dto.ProviderPlanData;
import com.fever.plans.repository.PlanRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Verifies synchronization semantics against the same PostgreSQL engine used at runtime. */
@SpringBootTest(properties = "provider.initial-delay=PT24H")
@Testcontainers
class PlanSynchronizationIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("feverplans")
            .withUsername("fever")
            .withPassword("fever");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    PlanProvider provider;

    @Autowired
    PlanSynchronizationService synchronizationService;

    @Autowired
    PlanRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void scheduledSynchronizationPersistsUpdatesAndRetainsMissingPlans() {
        when(provider.fetchPlans()).thenReturn(List.of(
                plan("291", "291", "online", "Original title", "2021-06-30T22:00:00"),
                plan("322", "1642", "online", "Historical plan", "2021-02-10T21:30:00")));

        synchronizationService.scheduledSync();

        when(provider.fetchPlans()).thenReturn(List.of(
                plan("291", "291", "online", "Updated title", "2021-06-30T23:00:00")));

        synchronizationService.scheduledSync();

        assertThat(repository.findAll()).hasSize(2);
        assertThat(repository.findByBasePlanIdAndProviderPlanId("291", "291"))
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getTitle()).isEqualTo("Updated title");
                    assertThat(updated.getEndsAt())
                            .isEqualTo(LocalDateTime.parse("2021-06-30T23:00:00"));
                });
        assertThat(repository.findByBasePlanIdAndProviderPlanId("322", "1642")).isPresent();
    }

    @Test
    void databaseFailureRollsBackTheCompleteSnapshot() {
        when(provider.fetchPlans()).thenReturn(List.of(
                plan("291", "291", "online", "Valid plan", "2021-06-30T22:00:00"),
                plan("322", "1642", "online", null, "2021-02-10T21:30:00")));

        assertThatThrownBy(synchronizationService::sync).isInstanceOf(RuntimeException.class);

        assertThat(repository.count()).isZero();
    }

    private ProviderPlanData plan(
            String basePlanId,
            String planId,
            String sellMode,
            String title,
            String endsAt) {
        return new ProviderPlanData(
                basePlanId,
                planId,
                sellMode,
                title,
                LocalDateTime.parse("2021-01-01T20:00:00"),
                LocalDateTime.parse(endsAt),
                List.of(new BigDecimal("10.00"), new BigDecimal("20.00")));
    }
}
