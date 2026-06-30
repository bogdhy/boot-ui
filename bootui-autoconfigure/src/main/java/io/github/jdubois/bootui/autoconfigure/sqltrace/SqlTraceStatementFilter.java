package io.github.jdubois.bootui.autoconfigure.sqltrace;

import io.github.jdubois.bootui.autoconfigure.filter.OttlExpressionFilter;

import java.util.List;

final class SqlTraceStatementFilter {

    private SqlTraceStatementFilter() {
    }

    static List<OttlExpressionFilter.Rule<SqlTraceRecorder.CapturedStatement>> compileExclusionRules(
        String[] expressions) {
        return OttlExpressionFilter.compile(expressions, SqlTraceStatementFilter::source);
    }

    static boolean isExcluded(
        SqlTraceRecorder.CapturedStatement statement,
        List<OttlExpressionFilter.Rule<SqlTraceRecorder.CapturedStatement>> rules) {
        return OttlExpressionFilter.matchesAny(statement, rules);
    }

    private static OttlExpressionFilter.ValueSource<SqlTraceRecorder.CapturedStatement> source(String source) {
        if ("sql".equals(source) || "db.statement".equals(source) || "statement".equals(source)) {
            return SqlTraceRecorder.CapturedStatement::sql;
        }
        if ("category".equals(source)) {
            return statement -> statement.category().name();
        }
        if ("statement.type".equals(source)) {
            return statement -> statement.statementType().name();
        }
        if ("thread".equals(source)) {
            return SqlTraceRecorder.CapturedStatement::thread;
        }
        if ("connection.id".equals(source)) {
            return SqlTraceRecorder.CapturedStatement::connectionId;
        }
        if ("trace.id".equals(source)) {
            return SqlTraceRecorder.CapturedStatement::traceId;
        }
        return null;
    }
}
