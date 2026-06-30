package io.github.jdubois.bootui.autoconfigure.otlp;

import io.github.jdubois.bootui.autoconfigure.filter.OttlExpressionFilter;
import io.github.jdubois.bootui.autoconfigure.monitoring.BootUiSelfDataFilter;
import java.util.List;

public final class TelemetrySpanFilter {

    private TelemetrySpanFilter() {}

    public static boolean isSelfSpan(NormalizedSpan span, String apiPath) {
        return BootUiSelfDataFilter.forPaths("/bootui", apiPath).isBootUiSpan(span);
    }

    public static List<OttlExpressionFilter.Rule<NormalizedSpan>> compileExclusionRules(String[] expressions) {
        return OttlExpressionFilter.compile(expressions, TelemetrySpanFilter::source);
    }

    public static boolean isExcluded(NormalizedSpan span, List<OttlExpressionFilter.Rule<NormalizedSpan>> rules) {
        return OttlExpressionFilter.matchesAny(span, rules);
    }

    private static OttlExpressionFilter.ValueSource<NormalizedSpan> source(String source) {
        if ("name".equals(source)) {
            return NormalizedSpan::name;
        }
        if ("kind".equals(source)) {
            return NormalizedSpan::kind;
        }
        if ("service.name".equals(source)) {
            return NormalizedSpan::serviceName;
        }
        if ("scope".equals(source) || "instrumentation.scope.name".equals(source)) {
            return NormalizedSpan::scope;
        }
        if ("status.code".equals(source)) {
            return NormalizedSpan::statusCode;
        }
        if ("status.message".equals(source)) {
            return NormalizedSpan::statusMessage;
        }
        String prefix = "attributes[";
        if (source.startsWith(prefix) && source.endsWith("]")) {
            String key = unquote(source.substring(prefix.length(), source.length() - 1).strip());
            if (key == null || key.isBlank()) {
                return null;
            }
            return span -> {
                AttributeValue value = span.attributes().get(key);
                return value == null ? null : value.asString();
            };
        }
        return null;
    }

    private static String unquote(String value) {
        if (value == null || value.length() < 2) {
            return null;
        }
        char quote = value.charAt(0);
        if ((quote != '"' && quote != '\'') || value.charAt(value.length() - 1) != quote) {
            return null;
        }
        String body = value.substring(1, value.length() - 1);
        return body.replace("\\\"", "\"").replace("\\'", "'").replace("\\\\", "\\");
    }
}
