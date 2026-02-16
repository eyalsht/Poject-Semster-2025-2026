package server.report.services;

import common.enums.ReportType;
import common.report.ActivityReport;
import org.hibernate.Session;
import server.report.ReportRequestContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ActivityReportService implements server.report.ReportManager.ParamAwareReportService {

    @Override
    public ReportType getType() {
        return ReportType.ACTIVITY;
    }

    @Override
    public Object generate(ReportRequestContext ctx, Object... params) {

        LocalDate from = (LocalDate) params[0];
        LocalDate to   = (LocalDate) params[1];
        Integer cityId = (Integer) params[2]; // null => all cities

        // We compute directly from events for views (so it updates immediately)
        // and from purchases for business metrics.

        try (Session s = ctx.getSessionFactory().openSession()) {
            s.beginTransaction();

            // ---- maps count (business metric) ----
            Number mapsCount = (Number) s.createNativeQuery("""
                SELECT COUNT(*)
                FROM maps m
                WHERE (:cityId IS NULL OR m.city_id = :cityId)
            """).setParameter("cityId", cityId).uniqueResult();

            // ---- purchases metrics ----
            Number oneTime = (Number) s.createNativeQuery("""
                SELECT COUNT(*)
                FROM purchases p
                WHERE (:cityId IS NULL OR p.city_id = :cityId)
                  AND p.purchase_type = 'ONE_TIME'
                  AND p.purchase_date >= :fromDate
                  AND p.purchase_date <  :toDatePlus
            """)
                    .setParameter("cityId", cityId)
                    .setParameter("fromDate", java.sql.Date.valueOf(from))
                    .setParameter("toDatePlus", java.sql.Date.valueOf(to.plusDays(1)))
                    .uniqueResult();

            Number subs = (Number) s.createNativeQuery("""
                SELECT COUNT(*)
                FROM purchases p
                WHERE (:cityId IS NULL OR p.city_id = :cityId)
                  AND p.purchase_type = 'SUBSCRIPTION'
                  AND p.is_renewal = 0
                  AND p.purchase_date >= :fromDate
                  AND p.purchase_date <  :toDatePlus
            """)
                    .setParameter("cityId", cityId)
                    .setParameter("fromDate", java.sql.Date.valueOf(from))
                    .setParameter("toDatePlus", java.sql.Date.valueOf(to.plusDays(1)))
                    .uniqueResult();

            Number renewals = (Number) s.createNativeQuery("""
                SELECT COUNT(*)
                FROM purchases p
                WHERE (:cityId IS NULL OR p.city_id = :cityId)
                  AND p.purchase_type = 'SUBSCRIPTION'
                  AND p.is_renewal = 1
                  AND p.purchase_date >= :fromDate
                  AND p.purchase_date <  :toDatePlus
            """)
                    .setParameter("cityId", cityId)
                    .setParameter("fromDate", java.sql.Date.valueOf(from))
                    .setParameter("toDatePlus", java.sql.Date.valueOf(to.plusDays(1)))
                    .uniqueResult();

            // ---- city-enter views (map_id IS NULL) ----
            Number cityEnterTotal = (Number) s.createNativeQuery("""
                SELECT COUNT(*)
                FROM map_view_events v
                WHERE (:cityId IS NULL OR v.city_id = :cityId)
                  AND v.map_id IS NULL
                  AND v.viewed_at >= :fromTs
                  AND v.viewed_at <  :toTsPlus
            """)
                    .setParameter("cityId", cityId)
                    .setParameter("fromTs", from.atStartOfDay())
                    .setParameter("toTsPlus", to.plusDays(1).atStartOfDay())
                    .uniqueResult();

            // ---- per-city city-enter views (for combo text "Haifa (25)") ----
            List<Object[]> cityEnterRowsRaw = s.createNativeQuery("""
                SELECT c.id, c.name,
                       COALESCE(x.cnt, 0) AS city_enter_views
                FROM cities c
                LEFT JOIN (
                    SELECT city_id, COUNT(*) AS cnt
                    FROM map_view_events
                    WHERE map_id IS NULL
                      AND viewed_at >= :fromTs
                      AND viewed_at <  :toTsPlus
                    GROUP BY city_id
                ) x ON x.city_id = c.id
                ORDER BY c.name
            """)
                    .setParameter("fromTs", from.atStartOfDay())
                    .setParameter("toTsPlus", to.plusDays(1).atStartOfDay())
                    .getResultList();

            ArrayList<ActivityReport.CityEnterRow> cityEnterRows = new ArrayList<>();
            for (Object[] r : cityEnterRowsRaw) {
                int cid = ((Number) r[0]).intValue();
                String cname = (String) r[1];
                int cnt = ((Number) r[2]).intValue();
                cityEnterRows.add(new ActivityReport.CityEnterRow(cid, cname, cnt));
            }

            // ---- map rows (ALWAYS) ----
            // map views = map_id IS NOT NULL
            // downloads = map_download_events (subscriber only, like your scheduler)
            List<Object[]> mapRowsRaw = s.createNativeQuery("""
                SELECT m.id,
                       m.name,
                       c.id AS city_id,
                       c.name AS city_name,
                       COALESCE(d.dcnt, 0) AS downloads,
                       COALESCE(v.vcnt, 0) AS views
                FROM maps m
                JOIN cities c ON c.id = m.city_id
                LEFT JOIN (
                    SELECT map_id, COUNT(*) AS vcnt
                    FROM map_view_events
                    WHERE map_id IS NOT NULL
                      AND (:cityId IS NULL OR city_id = :cityId)
                      AND viewed_at >= :fromTs
                      AND viewed_at <  :toTsPlus
                    GROUP BY map_id
                ) v ON v.map_id = m.id
                LEFT JOIN (
                    SELECT map_id, COUNT(*) AS dcnt
                    FROM map_download_events
                    WHERE map_id IS NOT NULL
                      AND is_subscriber = 1
                      AND (:cityId IS NULL OR city_id = :cityId)
                      AND downloaded_at >= :fromTs
                      AND downloaded_at <  :toTsPlus
                    GROUP BY map_id
                ) d ON d.map_id = m.id
                WHERE (:cityId IS NULL OR m.city_id = :cityId)
                ORDER BY c.name, views DESC, downloads DESC, m.name
            """)
                    .setParameter("cityId", cityId)
                    .setParameter("fromTs", from.atStartOfDay())
                    .setParameter("toTsPlus", to.plusDays(1).atStartOfDay())
                    .getResultList();

            ArrayList<ActivityReport.MapRow> mapRows = new ArrayList<>();
            for (Object[] r : mapRowsRaw) {
                int mapId = ((Number) r[0]).intValue();
                String mapName = (String) r[1];
                int cid = ((Number) r[2]).intValue();
                String cname = (String) r[3];
                int downloadsCnt = ((Number) r[4]).intValue();
                int viewsCnt = ((Number) r[5]).intValue();
                mapRows.add(new ActivityReport.MapRow(mapId, mapName, cid, cname, downloadsCnt, viewsCnt));
            }

            s.getTransaction().commit();

            return new ActivityReport(
                    from, to,
                    mapsCount == null ? 0 : mapsCount.intValue(),
                    oneTime == null ? 0 : oneTime.intValue(),
                    subs == null ? 0 : subs.intValue(),
                    renewals == null ? 0 : renewals.intValue(),
                    cityEnterTotal == null ? 0 : cityEnterTotal.intValue(),
                    cityEnterRows,
                    mapRows
            );
        }
    }

    @Override
    public boolean supportsDailyRefresh() {
        return true;
    }
}
