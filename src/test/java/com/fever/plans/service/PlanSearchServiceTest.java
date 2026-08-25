package com.fever.plans.service;

import com.fever.plans.repository.PlanRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PlanSearchServiceTest {
    @Mock
    PlanRepository repository;

    @Test
    void rejectsEqualRangeBounds() {
        var startsAt = OffsetDateTime.parse("2021-07-21T17:32:28Z");
        var service = new PlanSearchService(repository);

        assertThatThrownBy(() -> service.search(startsAt, startsAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("starts_at must be before ends_at");
    }

    @Test
    void rejectsInvertedRangeBounds() {
        var startsAt = OffsetDateTime.parse("2021-07-21T18:32:28Z");
        var endsAt = OffsetDateTime.parse("2021-07-21T17:32:28Z");
        var service = new PlanSearchService(repository);

        assertThatThrownBy(() -> service.search(startsAt, endsAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("starts_at must be before ends_at");
    }
}
