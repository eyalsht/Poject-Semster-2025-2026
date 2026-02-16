package server.report.services;

import common.report.ActivityReport;
import common.enums.ReportType;
import org.hibernate.Session;
import server.report.ActivityStatsScheduler;
import server.report.ReportRequestContext;

import java.time.LocalDate;
import java.util.*;

public class ActivityReportService implements server.report.ReportManager.ParamAwareReportService {

    @Override
    public ReportType getType() {
        return ReportType.ACTIVITY;
    }

    @Override
    public Object generate(ReportRequestContext ctx, Object... params) {

        LocalDate from = (LocalDate) params[0];
        LocalDate to = (LocalDate) params[1];
        Integer cityId = (Integer) params[2]; // null => all cities

        // Ensure daily stats exist for requested range
        LocalDate day = from;
        while (!day.isAfter(to)) {
            ActivityStatsScheduler.aggregateDay(ctx.getSessionFactory(), day);
            day = day.plusDays(1);
        }

        try (Session s = ctx.getSessionFactory().openSession()) {
            s.beginTransaction();

            // 1) City aggregation (always)
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

            // 2) Map table rows (only if SINGLE city requested)
            Integer tableCityId = null;
            String tableCityName = null;
            List<ActivityReport.MapRow> mapRows = Collections.emptyList();

            if (cityId != null) {
                tableCityId = cityId;

                // City name for header (optional)
                Object cityNameObj = s.createNativeQuery("SELECT name FROM cities WHERE id = :cid")
                        .setParameter("cid", cityId)
                        .uniqueResult();
                tableCityName = (cityNameObj == null) ? null : cityNameObj.toString();

                // views/downloads per map within range
                List<Object[]> mapStats = s.createNativeQuery("""
                    SELECT m.id,
                           m.name,
                           COALESCE(v.vcnt, 0) AS views,
                           COALESCE(d.dcnt, 0) AS downloads
                    FROM maps m
                    LEFT JOIN (
                        SELECT map_id, COUNT(*) AS vcnt
                        FROM map_view_events
                        WHERE city_id = :cid
                          AND map_id IS NOT NULL
                          AND viewed_at >= :fromTs
                          AND viewed_at <  :toTsPlus
                        GROUP BY map_id
                    ) v ON v.map_id = m.id
                    LEFT JOIN (
                        SELECT map_id, COUNT(*) AS dcnt
                        FROM map_download_events
                        WHERE city_id = :cid
                          AND map_id IS NOT NULL
                          AND is_subscriber = 1
                          AND downloaded_at >= :fromTs
                          AND downloaded_at <  :toTsPlus
                        GROUP BY map_id
                    ) d ON d.map_id = m.id
                    WHERE m.city_id = :cid
                    ORDER BY views DESC, downloads DESC, m.name
                """)
                        .setParameter("cid", cityId)
                        .setParameter("fromTs", from.atStartOfDay())
                        .setParameter("toTsPlus", to.plusDays(1).atStartOfDay())
                        .getResultList();

                ArrayList<ActivityReport.MapRow> tmp = new ArrayList<>();
                for (Object[] r : mapStats) {
                    int mid = ((Number) r[0]).intValue();
                    String mname = (String) r[1];
                    int vcnt = ((Number) r[2]).intValue();
                    int dcnt = ((Number) r[3]).intValue();
                    tmp.add(new ActivityReport.MapRow(mid, mname, vcnt, dcnt));
                }
                mapRows = tmp;
            }

            s.getTransaction().commit();
            return new ActivityReport(from, to, rows, tableCityId, tableCityName, mapRows);
        }
    }

    @Override
    public boolean supportsDailyRefresh() {
        return true;
    }

    @Override
    public void refreshDaily(ReportRequestContext ctx) {
        try {
            LocalDate today = LocalDate.now();
            ActivityStatsScheduler.aggregateDay(ctx.getSessionFactory(), today);
            System.out.println("[ReportManager] Activity daily stats aggregated for " + today);
        } catch (Exception e) {
            System.err.println("[ReportManager] Activity daily aggregation FAILED");
            e.printStackTrace();
        }
    }
}
