package io.github.jdubois.bootui.autoconfigure.sqltrace;

import io.github.jdubois.bootui.autoconfigure.filter.OttlExpressionFilter;
import io.github.jdubois.bootui.autoconfigure.sqltrace.SqlTraceRecorder.CapturedStatement;

import java.util.List;

final class SqlTraceStatementFilter {

    private SqlTraceStatementFilter() {
    }

    static List<OttlExpressionFilter.Rule<CapturedStatement>> compileExclusionRules(String[] expressions) {
        return OttlExpressionFilter.compile(expressions, SqlTraceStatementFilter::source);
    }

    static boolean isExcluded(
        CapturedStatement statement, List<OttlExpressionFilter.Rule<CapturedStatement>> rules) {
        return OttlExpressionFilter.matchesAny(statement, rules);
    }

    private static OttlExpressionFilter.ValueSource<CapturedStatement> source(String source) {
        final Source resolved = Source.from(source);
        return resolved == null ? null : resolved.valueSource;
    }

    private enum Source {
        SQL(CapturedStatement::sql, "sql", "db.statement", "statement"),
        CATEGORY(statement -> statement.category().name(), "category"),
        STATEMENT_TYPE(statement -> statement.statementType().name(), "statement.type"),
        THREAD(CapturedStatement::thread, "thread"),
        CONNECTION_ID(CapturedStatement::connectionId, "connection.id"),
        TRACE_ID(CapturedStatement::traceId, "trace.id");

        private final OttlExpressionFilter.ValueSource<CapturedStatement> valueSource;
        private final String[] aliases;

        Source(OttlExpressionFilter.ValueSource<CapturedStatement> valueSource, String... aliases) {
            this.valueSource = valueSource;
            this.aliases = aliases;
        }

        private static Source from(String source) {
            for (Source candidate : values()) {
                for (String alias : candidate.aliases) {
                    if (alias.equals(source)) {
                        return candidate;
                    }
                }
            }
            return null;
        }
    }
}
