package io.github.jdubois.bootui.autoconfigure.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class OttlExpressionFilter {

    private OttlExpressionFilter() {}

    public static <T> List<Rule<T>> compile(String[] expressions, SourceResolver<T> sourceResolver) {
        if (expressions == null || expressions.length == 0) {
            return List.of();
        }
        List<Rule<T>> rules = new ArrayList<>(expressions.length);
        for (String expression : expressions) {
            Rule<T> rule = parseExpression(expression, sourceResolver);
            if (rule != null) {
                rules.add(rule);
            }
        }
        return List.copyOf(rules);
    }

    public static <T> boolean matchesAny(T value, List<Rule<T>> rules) {
        if (value == null || rules == null || rules.isEmpty()) {
            return false;
        }
        for (Rule<T> rule : rules) {
            if (rule.matches(value)) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface Rule<T> {
        boolean matches(T value);
    }

    @FunctionalInterface
    public interface SourceResolver<T> {
        ValueSource<T> resolve(String source);
    }

    @FunctionalInterface
    public interface ValueSource<T> {
        String value(T value);
    }

    private static <T> Rule<T> parseExpression(String expression, SourceResolver<T> sourceResolver) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        List<String> parts = splitAnd(expression.strip());
        List<Rule<T>> conditions = new ArrayList<>(parts.size());
        for (String part : parts) {
            Rule<T> condition = parseCondition(part.strip(), sourceResolver);
            if (condition == null) {
                return null;
            }
            conditions.add(condition);
        }
        return value -> {
            for (Rule<T> condition : conditions) {
                if (!condition.matches(value)) {
                    return false;
                }
            }
            return true;
        };
    }

    private static <T> Rule<T> parseCondition(String expression, SourceResolver<T> sourceResolver) {
        if (expression.startsWith("IsMatch(") && expression.endsWith(")")) {
            String body = expression.substring("IsMatch(".length(), expression.length() - 1);
            int comma = indexOfTopLevel(body, ',');
            if (comma < 0) {
                return null;
            }
            ValueSource<T> source = sourceResolver.resolve(body.substring(0, comma).strip());
            String regex = unquote(body.substring(comma + 1).strip());
            if (source == null || regex == null) {
                return null;
            }
            try {
                Pattern pattern = Pattern.compile(regex);
                return value -> {
                    String text = source.value(value);
                    return text != null && pattern.matcher(text).find();
                };
            } catch (PatternSyntaxException ex) {
                return null;
            }
        }

        int equals = indexOfEquals(expression);
        if (equals < 0) {
            return null;
        }
        ValueSource<T> source = sourceResolver.resolve(expression.substring(0, equals).strip());
        String expected = unquote(expression.substring(equals + 2).strip());
        if (source == null || expected == null) {
            return null;
        }
        return value -> expected.equals(source.value(value));
    }

    private static int indexOfEquals(String value) {
        boolean quoted = false;
        char quote = 0;
        for (int i = 0; i < value.length() - 1; i++) {
            char c = value.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || value.charAt(i - 1) != '\\')) {
                if (!quoted) {
                    quoted = true;
                    quote = c;
                } else if (quote == c) {
                    quoted = false;
                }
            }
            if (!quoted && c == '=' && value.charAt(i + 1) == '=') {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfTopLevel(String value, char target) {
        boolean quoted = false;
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || value.charAt(i - 1) != '\\')) {
                if (!quoted) {
                    quoted = true;
                    quote = c;
                } else if (quote == c) {
                    quoted = false;
                }
            }
            if (!quoted && c == target) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> splitAnd(String expression) {
        List<String> parts = new ArrayList<>();
        boolean quoted = false;
        char quote = 0;
        int start = 0;
        for (int i = 0; i <= expression.length() - 5; i++) {
            char c = expression.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || expression.charAt(i - 1) != '\\')) {
                if (!quoted) {
                    quoted = true;
                    quote = c;
                } else if (quote == c) {
                    quoted = false;
                }
            }
            if (!quoted && expression.startsWith(" and ", i)) {
                parts.add(expression.substring(start, i));
                start = i + 5;
                i += 4;
            }
        }
        parts.add(expression.substring(start));
        return parts;
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
