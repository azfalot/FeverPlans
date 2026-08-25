package com.fever.plans.provider;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fever.plans.config.ProviderProperties;
import com.fever.plans.provider.dto.ProviderBasePlan;
import com.fever.plans.provider.dto.ProviderPlan;
import com.fever.plans.provider.dto.ProviderPlanData;
import com.fever.plans.provider.dto.ProviderResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Obtiene y parsea el XML del provider con reintentos acotados.
 *
 * <p>Los fallos del provider siguen aislados del endpoint {@code /search}: los reintentos solo
 * ocurren en la sincronización en background, nunca durante una petición de búsqueda.</p>
 */
@Component
class HttpPlanProvider implements PlanProvider {
    private static final Logger log = LoggerFactory.getLogger(HttpPlanProvider.class);

    private final HttpClient client;
    private final ProviderProperties properties;
    private final XmlMapper xmlMapper;

    HttpPlanProvider(HttpClient client, ProviderProperties properties) {
        this.client = client;
        this.properties = properties;
        this.xmlMapper = new XmlMapper();
    }

    @Override
    public List<ProviderPlanData> fetchPlans() {
        RuntimeException lastFailure = null;
        var attempts = Math.max(1, properties.maxAttempts());

        // Reintento acotado: damos margen a fallos transitorios, pero nunca entramos en un bucle infinito.
        for (var attempt = 1; attempt <= attempts; attempt++) {
            try {
                return fetchPlansOnce();
            } catch (RuntimeException exception) {
                lastFailure = exception;
                log.warn("Provider request attempt {}/{} failed: {}", attempt, attempts, exception.getMessage());
            }
        }
        throw lastFailure;
    }

    // Un intento real: llamada HTTP, validación de status code y parseo del XML.
    private List<ProviderPlanData> fetchPlansOnce() {
        try {
            var request = HttpRequest.newBuilder(URI.create(properties.url()))
                    .timeout(properties.readTimeout())
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Provider returned HTTP " + response.statusCode());
            }
            return parse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Provider request was interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Provider request failed", exception);
        }
    }

    List<ProviderPlanData> parse(String body) throws IOException {
        var document = xmlMapper.readValue(body, ProviderResponse.class);
        var providerPlans = new ArrayList<ProviderPlanData>();

        for (var basePlan : Optional.ofNullable(document.output).orElse(new ProviderResponse.Output()).basePlans) {
            for (var plan : basePlan.plans) {
                toProviderPlan(basePlan, plan).ifPresent(providerPlans::add);
            }
        }
        return providerPlans;
    }

    private Optional<ProviderPlanData> toProviderPlan(ProviderBasePlan basePlan, ProviderPlan plan) {
        try {
            return Optional.of(toProviderPlanWithValidDates(basePlan, plan));
        } catch (DateTimeParseException exception) {
            log.warn(
                    "Skipping provider plan {}/{} because it has an invalid date: {}",
                    basePlan.id,
                    plan.id,
                    exception.getParsedString());
            return Optional.empty();
        }
    }

    private ProviderPlanData toProviderPlanWithValidDates(ProviderBasePlan basePlan, ProviderPlan plan) {
        var prices = plan.zones.stream()
                .map(zone -> zone.price)
                .filter(Objects::nonNull)
                .toList();

        return new ProviderPlanData(
                basePlan.id,
                plan.id,
                basePlan.sellMode,
                basePlan.title,
                LocalDateTime.parse(plan.start),
                LocalDateTime.parse(plan.end),
                prices);
    }

}
