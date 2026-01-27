package server.report;

import common.enums.ReportType;

public interface ReportService
{

    ReportType getType();

    default boolean supportsDailyRefresh()
    {
        return false;
    }

    default void refreshDaily(ReportRequestContext ctx)
    {
        // no-op
    }
}
