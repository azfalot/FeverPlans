package com.fever.plans.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fever API error response")
public record ApiErrorResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ApiError error,
        @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED, example = "null") Object data) {

    public record ApiError(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message) {
    }
}
