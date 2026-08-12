package com.fever.plans.api;

import com.fever.plans.api.dto.SearchResponse;
import com.fever.plans.service.PlanSearchService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PlanController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                ServletWebSecurityAutoConfiguration.class
        }
)
class PlanControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    PlanSearchService searchService;

    @Test
    void returnsTheFeverResponseContract() throws Exception {
        var response = new SearchResponse(
                new SearchResponse.EventList(List.of(new SearchResponse.EventSummary(
                        UUID.randomUUID(), "Concert",
                        LocalDate.parse("2021-06-30"), LocalTime.parse("21:00:00"),
                        LocalDate.parse("2021-06-30"), LocalTime.parse("22:00:00"),
                        new BigDecimal("15.00"), new BigDecimal("30.00")))),
                null);
        when(searchService.search(any(), any())).thenReturn(response);

        mvc.perform(get("/search")
                        .param("starts_at", "2021-06-30T20:00:00Z")
                        .param("ends_at", "2021-06-30T23:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.events[0].title").value("Concert"))
                .andExpect(jsonPath("$.data.events[0].min_price").value(15));
    }

    @Test
    void rejectsInvalidTimestampParametersUsingTheErrorContract() throws Exception {
        mvc.perform(get("/search").param("starts_at", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void acceptsEqualBoundsBecauseTheContractOnlyDefinesStrictFiltering() throws Exception {
        when(searchService.search(any(), any()))
                .thenReturn(new SearchResponse(new SearchResponse.EventList(List.of()), null));

        mvc.perform(get("/search")
                        .param("starts_at", "2021-07-21T17:32:28Z")
                        .param("ends_at", "2021-07-21T17:32:28Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events").isEmpty())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));
    }
}
