package com.fever.plans.service;

import com.fever.plans.repository.PlanRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanSearchServiceTest {
    @Mock
    PlanRepository repository;

    @Test
    void appliesStrictBoundsEvenWhenBothRequestValuesAreEqual() {
        var startsAt = OffsetDateTime.parse("2021-07-21T17:32:28Z");
        var service = new PlanSearchService(repository);
        when(repository.findByStartsAtGreaterThanAndEndsAtLessThanOrderByStartsAtAsc(
                startsAt.toLocalDateTime(), startsAt.toLocalDateTime())).thenReturn(List.of());

        var response = service.search(startsAt, startsAt);

        assertThat(response.data().events()).isEmpty();
        verify(repository).findByStartsAtGreaterThanAndEndsAtLessThanOrderByStartsAtAsc(
                startsAt.toLocalDateTime(), startsAt.toLocalDateTime());
    }
}
