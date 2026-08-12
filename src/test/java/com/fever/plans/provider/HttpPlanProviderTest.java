package com.fever.plans.provider;

import com.fever.plans.config.ProviderProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpPlanProviderTest {
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void skipsAnInvalidPlanAndKeepsOtherValidPlansInTheSameSnapshot() throws Exception {
        var plans = provider(Duration.ofSeconds(1)).parse("""
                <planList version="1.0">
                  <output>
                    <base_plan base_plan_id="291" sell_mode="online" title="Camela">
                      <plan plan_id="291" plan_start_date="2021-06-30T21:00:00" plan_end_date="2021-06-30T22:00:00">
                        <zone price="20.00" />
                      </plan>
                    </base_plan>
                    <base_plan base_plan_id="444" sell_mode="online" title="Tributo">
                      <plan plan_id="1642" plan_start_date="2021-09-31T20:00:00" plan_end_date="2021-09-31T21:00:00">
                        <zone price="65.00" />
                      </plan>
                    </base_plan>
                  </output>
                </planList>
                """);

        assertThat(plans).singleElement().satisfies(plan -> {
            assertThat(plan.basePlanId()).isEqualTo("291");
            assertThat(plan.sellMode()).isEqualTo("online");
        });
    }

    @Test
    void failsWhenTheProviderReturnsAnHttpError() {
        server.createContext("/events", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();

        assertThatThrownBy(() -> provider(Duration.ofSeconds(1)).fetchPlans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Provider returned HTTP 503");
    }

    @Test
    void failsFastWhenTheProviderExceedsTheReadTimeout() {
        server.createContext("/events", exchange -> {
            try {
                Thread.sleep(300);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();

        assertThatThrownBy(() -> provider(Duration.ofMillis(50)).fetchPlans())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Provider request failed");
    }

    private HttpPlanProvider provider(Duration readTimeout) {
        return new HttpPlanProvider(
                HttpClient.newHttpClient(),
                new ProviderProperties(providerUrl(), Duration.ofSeconds(1), readTimeout, Duration.ofMinutes(15)));
    }

    private String providerUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/events";
    }
}
