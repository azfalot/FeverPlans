package com.fever.plans.repository;

import com.fever.plans.domain.Plan;
import com.fever.plans.domain.PlanId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Transactional
@EnabledIfSystemProperty(named = "integration", matches = "true")
class PlanRepositoryIntegrationTest {
    @Autowired
    private PlanRepository repository;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5433/feverplans");
        registry.add("spring.datasource.username", () -> "fever");
        registry.add("spring.datasource.password", () -> "fever");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    void persistsPlansAndSearchesThemUsingTheRealPostgresSchema() {
        repository.save(plan("it-291", "291", "Camela", "2031-06-30T21:00:00", "2031-06-30T22:00:00"));
        repository.save(plan("it-322", "1642", "Pantomima", "2031-07-01T21:00:00", "2031-07-01T22:00:00"));
        repository.save(plan("it-1591", "1642", "Los Morancos", "2031-08-01T21:00:00", "2031-08-01T22:00:00"));

        var existingPlans = repository.findByBasePlanIdIn(Set.of("it-291", "it-322"));
        var julyPlans = repository.findByStartsAtGreaterThanAndEndsAtLessThanOrderByStartsAtAsc(
                LocalDateTime.parse("2031-06-30T23:00:00"),
                LocalDateTime.parse("2031-07-02T00:00:00"));

        assertThat(existingPlans)
                .extracting(Plan::getTitle)
                .containsExactlyInAnyOrder("Camela", "Pantomima");
        assertThat(julyPlans)
                .singleElement()
                .extracting(Plan::getTitle)
                .isEqualTo("Pantomima");
    }

    private Plan plan(
            String basePlanId,
            String providerPlanId,
            String title,
            String startsAt,
            String endsAt) {
        return new Plan(
                new PlanId(basePlanId, providerPlanId),
                title,
                LocalDateTime.parse(startsAt),
                LocalDateTime.parse(endsAt),
                new BigDecimal("10.00"),
                new BigDecimal("20.00"));
    }
}
