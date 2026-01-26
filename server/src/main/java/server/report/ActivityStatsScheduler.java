package server.report;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActivityStatsScheduler {

    private static final ScheduledExecutorService exec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ActivityStatsScheduler");
                t.setDaemon(true); // does not block shutdown
                return t;
            });

    /**
     * Start a daily job that runs at 23:59 (server time zone) and aggregates stats for "today".
     */
    public static void start(SessionFactory sf) {

        // schedule first run at the next 23:59
        long initialDelaySeconds = secondsUntilNextRun(23, 59);
        long periodSeconds = TimeUnit.DAYS.toSeconds(1);

        exec.scheduleAtFixedRate(() -> safeAggregate(sf),
                initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
    }

    // ---------------------- internal helpers ----------------------

    private static long secondsUntilNextRun(int hour, int minute) {
        ZoneId zone = ZoneId.systemDefault(); // IMPORTANT: server timezone
        LocalDateTime now = LocalDateTime.now(zone);

        LocalDateTime next = now.withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);

        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }

        return Duration.between(now, next).getSeconds();
    }

    private static void safeAggregate(SessionFactory sf) {
        try {
            LocalDate today = LocalDate.now();
            aggregateDay(sf, today);
            System.out.println("[ActivityStatsScheduler] Aggregated stats for " + today);
        } catch (Exception e) {
            System.err.println("[ActivityStatsScheduler] FAILED");
            e.printStackTrace();
        }
    }

    /**
     * Aggregates all stats for one day into daily_city_activity_stats.
     * Day definition: [day 00:00, next day 00:00)
     */
    public static void aggregateDay(SessionFactory sf, LocalDate day) {

        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        try (Session s = sf.openSession()) {
            s.beginTransaction();

            // This inserts/updates one row per city for the chosen day.
            // It sums:
            // - one-time purchases (purchase_type='ONE_TIME')
            // - subscriptions (purchase_type='SUBSCRIPTION' and is_renewal=0)
            // - renewals (purchase_type='SUBSCRIPTION' and is_renewal=1)
            // - views from map_view_events
            // - downloads (subscribers only) from map_download_events
            s.createNativeQuery("""
                INSERT INTO daily_city_activity_stats
                    (city_id, stat_date, one_time_purchases, subscriptions, subscription_renewals, views, downloads)
                SELECT
                    c.id,
                    :statDate,

                    (SELECT COUNT(*)
                       FROM purchases p
                      WHERE p.city_id = c.id
                        AND p.purchase_type = 'ONE_TIME'
                        AND p.purchase_date >= :dayDate
                        AND p.purchase_date < :dayDatePlus1),

                    (SELECT COUNT(*)
                       FROM purchases p
                      WHERE p.city_id = c.id
                        AND p.purchase_type = 'SUBSCRIPTION'
                        AND p.is_renewal = 0
                        AND p.purchase_date >= :dayDate
                        AND p.purchase_date < :dayDatePlus1),

                    (SELECT COUNT(*)
                       FROM purchases p
                      WHERE p.city_id = c.id
                        AND p.purchase_type = 'SUBSCRIPTION'
                        AND p.is_renewal = 1
                        AND p.purchase_date >= :dayDate
                        AND p.purchase_date < :dayDatePlus1),

                    (SELECT COUNT(*)
                       FROM map_view_events v
                      WHERE v.city_id = c.id
                        AND v.viewed_at >= :startTs
                        AND v.viewed_at < :endTs),

                    (SELECT COUNT(*)
                       FROM map_download_events d
                      WHERE d.city_id = c.id
                        AND d.is_subscriber = 1
                        AND d.downloaded_at >= :startTs
                        AND d.downloaded_at < :endTs)

                FROM cities c
                ON DUPLICATE KEY UPDATE
                    one_time_purchases = VALUES(one_time_purchases),
                    subscriptions = VALUES(subscriptions),
                    subscription_renewals = VALUES(subscription_renewals),
                    views = VALUES(views),
                    downloads = VALUES(downloads)
            """)
                    .setParameter("statDate", java.sql.Date.valueOf(day))
                    .setParameter("dayDate", java.sql.Date.valueOf(day))
                    .setParameter("dayDatePlus1", java.sql.Date.valueOf(day.plusDays(1)))
                    .setParameter("startTs", start)
                    .setParameter("endTs", end)
                    .executeUpdate();

            s.getTransaction().commit();
        }
    }
}
