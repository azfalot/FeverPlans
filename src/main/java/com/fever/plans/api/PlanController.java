package com.fever.plans.api;

import com.fever.plans.api.dto.ApiErrorResponse;
import com.fever.plans.api.dto.SearchResponse;
import com.fever.plans.service.PlanSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlanController {
    private final PlanSearchService searchService;

    public PlanController(PlanSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "searchEvents", summary = "Lists the available events on a time range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of plans",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @ApiResponse(responseCode = "400", description = "The request was not correctly formed (missing required parameters, wrong types...)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Generic error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public SearchResponse search(
            @RequestParam(value = "starts_at", required = false)
            @Parameter(description = "Return only events that starts after this date",
                    schema = @Schema(type = "string", format = "date-time", example = "2017-07-21T17:32:28Z"))
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startsAt,
            @RequestParam(value = "ends_at", required = false)
            @Parameter(description = "Return only events that finishes before this date",
                    schema = @Schema(type = "string", format = "date-time", example = "2021-07-21T17:32:28Z"))
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endsAt) {
        return searchService.search(startsAt, endsAt);
    }
}
