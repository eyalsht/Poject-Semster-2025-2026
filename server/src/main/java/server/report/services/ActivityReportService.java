package server.report.services;

import common.report.ActivityReport;
import common.enums.ReportType;
import org.hibernate.Session;
import server.report.ActivityStatsScheduler;
import server.report.ReportManager;
import server.report.ReportRequestContext;

import java.time.LocalDate;
import java.util.*;

public class ActivityReportService implements ReportManager.ParamAwareReportService {

    @Override
    public ReportType getType() {
        return ReportType.ACTIVITY;
    }

    @Override
    public Object generate(ReportRequestContext ctx, Object... params) {

        LocalDate from = (LocalDate) params[0];
        LocalDate to = (LocalDate) params[1];
        Integer cityId = (Integer) params[2];

        try (Session s = ctx.getSessionFactory().openSession()) {
            s.beginTransaction();

            List<Object[]> stats = s.createNativeQuery("""
                SELECT c.id, c.name,
                       COALESCE(SUM(d.one_time_purchases),0),
                       COALESCE(SUM(d.subscriptions),0),
                       COALESCE(SUM(d.subscription_renewals),0),
                       COALESCE(SUM(d.views),0),
                       COALESCE(SUM(d.downloads),0)
                FROM cities c
                LEFT JOIN daily_city_activity_stats d
                       ON d.city_id = c.id
                      AND d.stat_date >= :fromDate
                      AND d.stat_date <= :toDate
                WHERE (:cityId IS NULL OR c.id = :cityId)
                GROUP BY c.id, c.name
                ORDER BY c.name
            """)
                    .setParameter("fromDate", java.sql.Date.valueOf(from))
                    .setParameter("toDate", java.sql.Date.valueOf(to))
                    .setParameter("cityId", cityId)
                    .getResultList();

            Map<Integer, Integer> mapsCount = new HashMap<>();
            List<Object[]> maps = s.createNativeQuery("""
                SELECT m.city_id, COUNT(*)
                FROM maps m
                GROUP BY m.city_id
            """).getResultList();

            for (Object[] r : maps) {
                mapsCount.put(((Number) r[0]).intValue(), ((Number) r[1]).intValue());
            }

            List<ActivityReport.CityRow> rows = new ArrayList<>();
            for (Object[] r : stats) {
                int cid = ((Number) r[0]).intValue();
                String cname = (String) r[1];
                int oneTime = ((Number) r[2]).intValue();
                int subs = ((Number) r[3]).intValue();
                int renew = ((Number) r[4]).intValue();
                int views = ((Number) r[5]).intValue();
                int downloads = ((Number) r[6]).intValue();
                int mapsN = mapsCount.getOrDefault(cid, 0);

                rows.add(new ActivityReport.CityRow(cid, cname, mapsN, oneTime, subs, renew, views, downloads));
            }

            s.getTransaction().commit();
            return new ActivityReport(from, to, rows);
        }
    }

    @Override
    public boolean supportsDailyRefresh() {
        return true;
    }

    @Override
    public void refreshDaily(ReportRequestContext ctx) {
        // aggregate stats for "today" into daily_city_activity_stats
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            ActivityStatsScheduler.aggregateDay(ctx.getSessionFactory(), today);
            System.out.println("[ReportManager] Activity daily stats aggregated for " + today);
        } catch (Exception e) {
            System.err.println("[ReportManager] Activity daily aggregation FAILED");
            e.printStackTrace();
        }
    }

}
