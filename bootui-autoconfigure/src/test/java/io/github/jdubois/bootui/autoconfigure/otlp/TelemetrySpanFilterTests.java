package io.github.jdubois.bootui.autoconfigure.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TelemetrySpanFilterTests {

    @Test
    void matchesSpanNameAndAttributes() {
        var rules = TelemetrySpanFilter.compileExclusionRules(new String[] {
            "IsMatch(attributes[\"db.statement\"], \".*(token_entry|_event_entry|saga_entry).*\")",
            "IsMatch(name, \".*org\\\\.axonframework.*TokenEntry.*\")"
        });

        assertThat(TelemetrySpanFilter.isExcluded(span("SELECT token_entry", Map.of("db.statement", "select token_entry")), rules))
                .isTrue();
        assertThat(TelemetrySpanFilter.isExcluded(span("org.axonframework.TokenEntry.load", Map.of()), rules))
                .isTrue();
        assertThat(TelemetrySpanFilter.isExcluded(span("GET /api/orders", Map.of("http.route", "/api/orders")), rules))
                .isFalse();
    }

    @Test
    void matchesExactNameAndCompoundAttributeExpression() {
        var rules = TelemetrySpanFilter.compileExclusionRules(new String[] {
            "name == \"Transaction.commit\"",
            "attributes[\"http.route\"] == \"/*\" and attributes[\"http.method\"] == \"GET\""
        });

        assertThat(TelemetrySpanFilter.isExcluded(span("Transaction.commit", Map.of()), rules)).isTrue();
        assertThat(TelemetrySpanFilter.isExcluded(
                        span("GET /*", Map.of("http.route", "/*", "http.method", "GET")), rules))
                .isTrue();
        assertThat(TelemetrySpanFilter.isExcluded(
                        span("POST /*", Map.of("http.route", "/*", "http.method", "POST")), rules))
                .isFalse();
    }

    @Test
    void ignoresInvalidSpanExclusionExpressions() {
        var rules = TelemetrySpanFilter.compileExclusionRules(new String[] {"IsMatch(name, \"[\")", "unknown == \"x\""});

        assertThat(rules).isEmpty();
        assertThat(TelemetrySpanFilter.isExcluded(span("anything", Map.of()), rules)).isFalse();
    }

    private static NormalizedSpan span(String name, Map<String, String> attributes) {
        return new NormalizedSpan(
                "trace",
                "span",
                null,
                name,
                "INTERNAL",
                "sample",
                "test",
                1,
                2,
                "OK",
                null,
                toAttributeValues(attributes),
                List.of());
    }

    private static Map<String, AttributeValue> toAttributeValues(Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> AttributeValue.ofString(entry.getValue())));
    }
}
