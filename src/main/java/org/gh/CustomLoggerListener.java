package org.gh;

import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.impl.DefaultVisitListener;
import org.jooq.tools.JooqLogger;
import org.jooq.tools.LoggerListener;
import org.jooq.tools.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.jooq.impl.DSL.*;
import static org.jooq.tools.StringUtils.abbreviate;

@Component
public class CustomLoggerListener extends DefaultExecuteListener {

    private static final JooqLogger log       = JooqLogger.getLogger(CustomLoggerListener.class);
    private static final String     BUFFER    = "org.jooq.tools.LoggerListener.BUFFER";
    private static final String     DO_BUFFER = "org.jooq.tools.LoggerListener.DO_BUFFER";

    @Override
    public void renderEnd(ExecuteContext ctx) {
        if (log.isDebugEnabled()) {
            Configuration configuration = ctx.configuration();
            String newline = TRUE.equals(configuration.settings().isRenderFormatted()) ? "\n" : "";

            // [#2939] Prevent excessive logging of bind variables only in DEBUG mode, not in TRACE mode.
            if (!log.isTraceEnabled())
                configuration = configuration.deriveAppending(new CustomLoggerListener.BindValueAbbreviator());

            String[] batchSQL = ctx.batchSQL();
            if (ctx.query() != null) {

                // Actual SQL passed to JDBC
                log.debug("Executing query", newline + ctx.sql());

                // [#1278] DEBUG log also SQL with inlined bind values, if
                // that is not the same as the actual SQL passed to JDBC
                String inlined = DSL.using(configuration).renderInlined(ctx.query());
                if (!ctx.sql().equals(inlined))
                    log.debug("-> with bind values", newline + inlined);
            }

            // [#2987] Log routines
            else if (ctx.routine() != null) {
                log.debug("Calling routine", newline + ctx.sql());

                String inlined = DSL.using(configuration)
                        .renderInlined(ctx.routine());

                if (!ctx.sql().equals(inlined))
                    log.debug("-> with bind values", newline + inlined);
            }

            else if (!StringUtils.isBlank(ctx.sql())) {

                // [#1529] Batch queries should be logged specially
                if (ctx.type() == ExecuteType.BATCH)
                    log.debug("Executing batch query", newline + ctx.sql());
                else
                    log.debug("Executing query", newline + ctx.sql());
            }

            // [#2532] Log a complete BatchMultiple query
            else if (batchSQL.length > 0) {
                if (batchSQL[batchSQL.length - 1] != null)
                    for (String sql : batchSQL)
                        log.debug("Executing batch query", newline + sql);
            }
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public void recordEnd(ExecuteContext ctx) {

        // [#12564] Make sure we don't log any nested-level results or records
        if (ctx.recordLevel() > 0)
            return;

        if (log.isTraceEnabled() && ctx.record() != null)
            logMultiline("Record fetched", ctx.record().toString(), Level.FINER);

        // [#10019] Buffer the first few records
        if (log.isDebugEnabled() && ctx.record() != null && !FALSE.equals(ctx.data(DO_BUFFER))) {
            Result<Record> buffer = (Result<Record>) ctx.data(BUFFER);

            if (buffer == null)
                ctx.data(BUFFER, buffer = ctx.dsl().newResult(ctx.record().fields()));

            if (buffer.size() < maxRows())
                buffer.add(ctx.record());
        }
    }

    @Override
    public void resultStart(ExecuteContext ctx) {

        // [#10019] Don't buffer any records if it isn't needed
        ctx.data(DO_BUFFER, false);
    }

    @Override
    public void resultEnd(ExecuteContext ctx) {

        // [#12564] Make sure we don't log any nested-level results or records
        if (ctx.resultLevel() > 0)
            return;

        if (ctx.result() != null && log.isDebugEnabled()) {
            log(ctx.configuration(), ctx.result());
            log.debug("Fetched row(s)", ctx.result().size());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void fetchEnd(ExecuteContext ctx) {
        Result<Record> buffer = (Result<Record>) ctx.data(BUFFER);

        if (buffer != null && !buffer.isEmpty()) {
            log(ctx.configuration(), buffer);
            log.debug("Fetched row(s)", buffer.size() + (buffer.size() < maxRows() ? "" : " (or more)"));
        }
    }

    private void log(Configuration configuration, Result<?> result) {
        logMultiline("Fetched result", result.format(configuration.formattingProvider().txtFormat().maxRows(1000000).maxColWidth(maxColWidth())), Level.FINE);
    }

    private int maxRows() {
        if (log.isTraceEnabled())
            return 500;
        else if (log.isDebugEnabled())
            return 5;
        else
            return 0;
    }

    private int maxColWidth() {
        if (log.isTraceEnabled())
            return 500;
        else if (log.isDebugEnabled())
            return 50;
        else
            return 0;
    }

    @Override
    public void executeEnd(ExecuteContext ctx) {
        if (ctx.rows() >= 0)
            if (log.isDebugEnabled())
                log.debug("Affected row(s)", ctx.rows());
    }

    @Override
    public void outEnd(ExecuteContext ctx) {
        if (ctx.routine() != null)
            if (log.isDebugEnabled())
                logMultiline("Fetched OUT parameters", "" + StringUtils.defaultIfNull(record(ctx.configuration(), ctx.routine()), "N/A"), Level.FINE);
    }

    @Override
    public void exception(ExecuteContext ctx) {
        if (log.isDebugEnabled())
            log.debug("Exception", ctx.exception());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Record record(Configuration configuration, Routine<?> routine) {
        Record result = null;

        List<Field<?>> fields = new ArrayList<>(1 + routine.getOutParameters().size());
        Parameter<?> returnParam = routine.getReturnParameter();
        if (returnParam != null)
            fields.add(field(name(returnParam.getName()), returnParam.getDataType()));

        for (Parameter<?> param : routine.getOutParameters())
            fields.add(field(name(param.getName()), param.getDataType()));

        if (fields.size() > 0) {
            result = DSL.using(configuration).newRecord(fields.toArray(new Field[0]));

            int i = 0;
            if (returnParam != null)
                result.setValue((Field) fields.get(i++), routine.getValue(returnParam));

            for (Parameter<?> param : routine.getOutParameters())
                result.setValue((Field) fields.get(i++), routine.getValue(param));

            result.changed(false);
        }

        return result;
    }

    private void logMultiline(String comment, String message, Level level) {
        for (String line : message.split("\n")) {
            if (level == Level.FINE) {
                log.debug(comment, line);
            }
            else {
                log.trace(comment, line);
            }

            comment = "";
        }
    }

    private static final int maxLength = 2000;

    private static class BindValueAbbreviator extends DefaultVisitListener {

        private boolean anyAbbreviations = false;

        @Override
        public void visitStart(VisitContext context) {
            if (context.renderContext() != null) {
                QueryPart part = context.queryPart();

                if (part instanceof Param) { Param<?> param = (Param<?>) part;
                    Object value = param.getValue();

                    if (value instanceof String && ((String) value).length() > maxLength) {
                        anyAbbreviations = true;
                        context.queryPart(val(abbreviate((String) value, maxLength)));
                    }
                    else if (value instanceof byte[] && ((byte[]) value).length > maxLength) {
                        anyAbbreviations = true;
                        context.queryPart(val(Arrays.copyOf((byte[]) value, maxLength)));
                    }
                }
            }
        }

        @Override
        public void visitEnd(VisitContext context) {
            if (anyAbbreviations && context.queryPartsLength() == 1)
                context.renderContext().sql(" -- Bind values may have been abbreviated for DEBUG logging. Use TRACE logging for very large bind variables.");
        }
    }
}
