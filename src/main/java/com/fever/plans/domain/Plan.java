package com.fever.plans.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persisted snapshot of a provider plan that was observed as available online.
 *
 * <p>The provider natural key is stored alongside the stable API UUID so imports can update an
 * existing plan without relying on a provider-generated UUID.</p>
 */
@Entity
@Table(
        name = "plans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"base_plan_id", "provider_plan_id"}),
        indexes = {
            @Index(name = "idx_plans_starts_at", columnList = "starts_at"),
            @Index(name = "idx_plans_ends_at", columnList = "ends_at")
        })
public class Plan {
    @Id
    private UUID id;

    @Column(name = "base_plan_id", nullable = false)
    private String basePlanId;

    @Column(name = "provider_plan_id", nullable = false)
    private String providerPlanId;

    @Column(nullable = false)
    private String title;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "min_price", precision = 12, scale = 2)
    private BigDecimal minPrice;

    @Column(name = "max_price", precision = 12, scale = 2)
    private BigDecimal maxPrice;

    protected Plan() {
    }

    public Plan(
            PlanId planId,
            String title,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        this.id = planId.asUuid();
        this.basePlanId = planId.basePlanId();
        this.providerPlanId = planId.providerPlanId();
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public UUID getId() {
        return id;
    }

    public String getBasePlanId() {
        return basePlanId;
    }

    public String getProviderPlanId() {
        return providerPlanId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void update(
            String title,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }
}
