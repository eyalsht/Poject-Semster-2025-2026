package server.report.services;

import common.enums.ReportType;
import common.report.PurchasesReport;
import org.hibernate.Session;
import server.report.ReportManager;
import server.report.ReportRequestContext;
import server.report.ActivityStatsScheduler;

import java.time.LocalDate;
import java.util.List;

public class PurchasesReportService implements ReportManager.ParamAwareReportService {

    @Override
    public ReportType getType() {
        return ReportType.PURCHASES;
    }

    @Override
    public Object generate(ReportRequestContext ctx, Object... params)
    {

        LocalDate from = (LocalDate) params[0];
        LocalDate to = (LocalDate) params[1];
        Integer cityId = (Integer) params[2];

        LocalDate day = from;
        while (!day.isAfter(to)) {
            server.report.ActivityStatsScheduler.aggregateDay(ctx.getSessionFactory(), day);
            day = day.plusDays(1);
        }

        try (Session s = ctx.getSessionFactory().openSession()) {
            s.beginTransaction();

            // totals in range
            Object[] row = (Object[]) s.createNativeQuery("""
                SELECT
                    COALESCE(SUM(d.one_time_purchases),0) AS one_time,
                    COALESCE(SUM(d.subscriptions),0) AS subs,
                    COALESCE(SUM(d.subscription_renewals),0) AS renewals
                FROM daily_city_activity_stats d
                WHERE d.city_id = :cityId
                  AND d.stat_date >= :fromDate
                  AND d.stat_date <= :toDate
            """)
                    .setParameter("cityId", cityId)
                    .setParameter("fromDate", java.sql.Date.valueOf(from))
                    .setParameter("toDate", java.sql.Date.valueOf(to))
                    .getSingleResult();

            int oneTime = ((Number) row[0]).intValue();
            int subs = ((Number) row[1]).intValue();
            int renew = ((Number) row[2]).intValue();

            // city name
            String cityName = (String) s.createNativeQuery("""
                SELECT c.name FROM cities c WHERE c.id = :cityId
            """)
                    .setParameter("cityId", cityId)
                    .getSingleResult();

            s.getTransaction().commit();

            return new PurchasesReport(from, to, cityId, cityName, oneTime, subs, renew);
        }
    }

    @Override
    public boolean supportsDailyRefresh() {
        return false; // uses already aggregated table; no special daily job
    }

    @Override
    public void refreshDaily(ReportRequestContext ctx) {
        // no-op
    }
}
