package com.fever.plans.performance;

import com.fever.plans.provider.PlanProvider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in latency regression test for the complete HTTP-to-PostgreSQL search path.
 *
 * <p>Run with {@code mvn verify -Pperformance}. This is a repeatable local benchmark, not a
 * production capacity claim: infrastructure-level load testing is still required before sizing a
 * deployment.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "provider.initial-delay=PT24H")
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
@Testcontainers
class PlanSearchPerformanceIT {
    private static final int STORED_PLANS = 50_000;
    private static final int REQUESTS = 200;
    private static final int CONCURRENCY = 20;
    private static final long P95_LIMIT_MILLIS = 500;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("feverplans")
            .withUsername("fever")
            .withPassword("fever");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    PlanProvider provider;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @LocalServerPort
    int port;

    private HttpClient client;
    private HttpRequest request;

    @BeforeEach
    void prepareDataset() {
        jdbcTemplate.execute("truncate table plans");
        jdbcTemplate.execute("""
                insert into plans (
                    id, base_plan_id, provider_plan_id, title,
                    starts_at, ends_at, min_price, max_price)
                select
                    md5('performance-' || value)::uuid,
                    'performance',
                    value::text,
                    'Performance plan ' || value,
                    timestamp '2021-01-01 00:00:00' + value * interval '1 minute',
                    timestamp '2021-01-01 01:00:00' + value * interval '1 minute',
                    10.00,
                    20.00
                from generate_series(1, 50000) as value
                """);

        assertThat(jdbcTemplate.queryForObject("select count(*) from plans", Integer.class))
                .isEqualTo(STORED_PLANS);

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        request = HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + port
                                + "/search?starts_at=2021-01-15T00:00:00Z"
                                + "&ends_at=2021-01-15T00:15:00Z"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        for (var index = 0; index < 20; index++) {
            executeSearch();
        }
    }

    @Test
    void keepsP95WithinHundredsOfMillisecondsUnderConcurrentRequests() {
        var durations = Collections.synchronizedList(new ArrayList<Long>(REQUESTS));
        var startedAt = System.nanoTime();

        try (var executor = Executors.newFixedThreadPool(CONCURRENCY)) {
            List<CompletableFuture<Void>> executions = new ArrayList<>(REQUESTS);
            for (var index = 0; index < REQUESTS; index++) {
                executions.add(CompletableFuture.runAsync(
                        () -> durations.add(executeSearch()),
                        executor));
            }
            CompletableFuture.allOf(executions.toArray(CompletableFuture[]::new)).join();
        }

        var elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        var sortedDurations = durations.stream().sorted().toList();
        var p95 = sortedDurations.get((int) Math.ceil(sortedDurations.size() * 0.95) - 1);
        var throughput = REQUESTS / elapsedSeconds;

        System.out.printf(
                "Search benchmark: rows=%d requests=%d concurrency=%d p95=%dms throughput=%.1f req/s%n",
                STORED_PLANS,
                REQUESTS,
                CONCURRENCY,
                p95,
                throughput);

        assertThat(durations).hasSize(REQUESTS);
        assertThat(p95).isLessThan(P95_LIMIT_MILLIS);
    }

    private long executeSearch() {
        try {
            var startedAt = System.nanoTime();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            var durationMillis = (System.nanoTime() - startedAt) / 1_000_000;

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"error\":null");
            return durationMillis;
        } catch (Exception exception) {
            throw new IllegalStateException("Performance request failed", exception);
        }
    }
}
