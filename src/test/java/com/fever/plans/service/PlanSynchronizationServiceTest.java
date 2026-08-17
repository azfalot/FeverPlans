package com.fever.plans.service;

import com.fever.plans.domain.Plan;
import com.fever.plans.provider.PlanProvider;
import com.fever.plans.provider.dto.ProviderPlanData;
import com.fever.plans.repository.PlanRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanSynchronizationServiceTest {
    @Mock
    PlanProvider provider;

    @Mock
    PlanRepository repository;

    private final Map<String, Plan> storedPlans = new HashMap<>();
    private PlanSynchronizationService synchronizationService;

    @BeforeEach
    void setUp() {
        synchronizationService = new PlanSynchronizationService(
                provider,
                new PlanImportService(repository));
        when(repository.findByBasePlanIdAndProviderPlanId(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(storedPlans.get(key(
                        invocation.getArgument(0), invocation.getArgument(1)))));
        when(repository.save(ArgumentMatchers.any(Plan.class))).thenAnswer(invocation -> {
            var plan = invocation.getArgument(0, Plan.class);
            storedPlans.put(key(plan.getBasePlanId(), plan.getProviderPlanId()), plan);
            return plan;
        });
    }

    @Test
    void preservesHistoricalOnlinePlansAndUpdatesMutableValuesAcrossConsecutiveSnapshots() {
        synchronize(responseOne());
        synchronize(responseTwo());
        synchronize(responseThree());

        assertThat(storedPlans).hasSize(3);
        assertThat(storedPlans).containsKey(key("322", "1642"));
        assertThat(storedPlans).doesNotContainKey(key("444", "1642"));
        assertThat(storedPlans.get(key("291", "291")).getEndsAt())
                .isEqualTo(LocalDateTime.parse("2021-06-30T21:30:00"));
        assertThat(storedPlans.get(key("1591", "1642")).getEndsAt())
                .isEqualTo(LocalDateTime.parse("2021-07-31T21:00:00"));
    }

    @Test
    void leavesExistingStateUntouchedWhenTheProviderFails() {
        synchronize(List.of(plan("291", "291", "online", "Camela", "2021-06-30T22:00:00")));
        when(provider.fetchPlans()).thenThrow(new IllegalStateException("Provider returned HTTP 503"));

        assertThatThrownBy(synchronizationService::sync).isInstanceOf(IllegalStateException.class);

        assertThat(storedPlans).hasSize(1).containsKey(key("291", "291"));
    }

    @Test
    void retainsTheLastOnlineVersionWhenTheProviderLaterMarksThePlanOffline() {
        synchronize(List.of(plan("291", "291", "online", "Camela", "2021-06-30T22:00:00")));
        synchronize(List.of(plan("291", "291", "offline", "Changed title", "2021-06-30T23:00:00")));

        var storedPlan = storedPlans.get(key("291", "291"));
        assertThat(storedPlan.getTitle()).isEqualTo("Camela");
        assertThat(storedPlan.getEndsAt()).isEqualTo(LocalDateTime.parse("2021-06-30T22:00:00"));
    }

    @Test
    void scheduledProviderFailureDoesNotPreventSearchingThePreviouslyStoredPlans() {
        synchronize(List.of(plan("291", "291", "online", "Camela", "2021-06-30T22:00:00")));
        when(provider.fetchPlans()).thenThrow(new IllegalStateException("Provider returned HTTP 503"));
        when(repository.findAllByOrderByStartsAtAsc()).thenReturn(List.copyOf(storedPlans.values()));

        synchronizationService.scheduledSync();
        var response = new PlanSearchService(repository).search(null, null);

        assertThat(response.data().events()).singleElement().satisfies(event -> {
            assertThat(event.title()).isEqualTo("Camela");
            assertThat(event.min_price()).isEqualByComparingTo("10");
        });
    }

    private void synchronize(List<ProviderPlanData> providerPlans) {
        when(provider.fetchPlans()).thenReturn(providerPlans);
        synchronizationService.sync();
    }

    private List<ProviderPlanData> responseOne() {
        return List.of(
                plan("291", "291", "online", "Camela", "2021-06-30T22:00:00"),
                plan("322", "1642", "online", "Pantomima", "2021-02-10T21:30:00"),
                plan("1591", "1642", "online", "Los Morancos", "2021-07-31T21:00:00"));
    }

    private List<ProviderPlanData> responseTwo() {
        return List.of(
                plan("291", "291", "online", "Camela", "2021-06-30T22:00:00"),
                plan("1591", "1642", "online", "Los Morancos", "2021-07-31T21:20:00"),
                plan("444", "1642", "offline", "Tributo", "2021-09-30T21:00:00"));
    }

    private List<ProviderPlanData> responseThree() {
        return List.of(
                plan("291", "291", "online", "Camela", "2021-06-30T21:30:00"),
                plan("1591", "1642", "online", "Los Morancos", "2021-07-31T21:00:00"));
    }

    private ProviderPlanData plan(String basePlanId, String planId, String sellMode, String title, String endsAt) {
        return new ProviderPlanData(
                basePlanId, planId, sellMode, title,
                LocalDateTime.parse("2021-06-30T21:00:00"),
                LocalDateTime.parse(endsAt),
                List.of(new BigDecimal("10"), new BigDecimal("20")));
    }

    private String key(String basePlanId, String planId) {
        return basePlanId + ":" + planId;
    }
}
