package com.fever.plans.service;

import com.fever.plans.api.dto.SearchResponse;
import com.fever.plans.domain.Plan;
import com.fever.plans.repository.PlanRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de búsqueda local: solo lee PostgreSQL y nunca llama al provider externo. */
@Service
public class PlanSearchService {
    private final PlanRepository repository;

    public PlanSearchService(PlanRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        validateRange(startsAt, endsAt);

        var events = findPlans(startsAt, endsAt).stream()
                .map(plan -> new SearchResponse.EventSummary(
                        plan.getId(),
                        plan.getTitle(),
                        plan.getStartsAt().toLocalDate(),
                        plan.getStartsAt().toLocalTime(),
                        plan.getEndsAt().toLocalDate(),
                        plan.getEndsAt().toLocalTime(),
                        plan.getMinPrice(),
                        plan.getMaxPrice()))
                .toList();

        return new SearchResponse(new SearchResponse.EventList(events), null);
    }

    private void validateRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        // Si el cliente manda las dos fechas, el rango debe tener sentido antes de consultar BD.
        if (startsAt != null && endsAt != null && !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("starts_at must be before ends_at");
        }
    }

    private java.time.LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private List<Plan> findPlans(
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {
        if (startsAt == null && endsAt == null) {
            return repository.findAllByOrderByStartsAtAsc();
        }
        if (startsAt == null) {
            return repository.findByEndsAtLessThanOrderByStartsAtAsc(toLocalDateTime(endsAt));
        }
        if (endsAt == null) {
            return repository.findByStartsAtGreaterThanOrderByStartsAtAsc(toLocalDateTime(startsAt));
        }
        return repository.findByStartsAtGreaterThanAndEndsAtLessThanOrderByStartsAtAsc(
                toLocalDateTime(startsAt),
                toLocalDateTime(endsAt));
    }
}
