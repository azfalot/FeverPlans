package com.fever.plans.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "SearchResponse")
public record SearchResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EventList data,
        @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED, example = "null") Object error) {

    public record EventList(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<EventSummary> events) {
    }

    public record EventSummary(
            @Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
            @Schema(format = "date", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate start_date,
            @Schema(type = "string", format = "time", nullable = true, example = "22:38:19",
                    requiredMode = Schema.RequiredMode.REQUIRED) LocalTime start_time,
            @Schema(format = "date", nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) LocalDate end_date,
            @Schema(type = "string", format = "time", nullable = true, example = "14:45:15",
                    requiredMode = Schema.RequiredMode.REQUIRED) LocalTime end_time,
            @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal min_price,
            @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal max_price) {
    }
}
