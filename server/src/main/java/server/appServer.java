package server;

import server.handler.HandlerRegistry;
import server.report.ActivityStatsScheduler;
import server.repository.PurchaseRepository;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class appServer {
    private static GcmServer gcmServer;

    public static void main(String[] args) throws IOException {
        try {
            HibernateUtil.initialize("dbHibernate.cfg.xml");
            System.out.println("✓ Database connected via Hibernate");

            // Ensure handlers + report manager are created
            HandlerRegistry.getInstance();
            System.out.println("✓ Handler registry initialized");

            System.out.println("Server ready to accept connections.");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize server:");
            e.printStackTrace();
        }

        gcmServer = new GcmServer(5555);
        System.out.println("Server Started and Listening on port 5555");
        gcmServer.listen();
        startDailyNotificationCheck();
        // Start daily refresh at 23:59 using the central ReportManager
        ActivityStatsScheduler.start(HandlerRegistry.getInstance().getReportManager());
    }

    private static void startDailyNotificationCheck() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        // setting the schedule to our time zone time - 8AM
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Jerusalem"));
        java.time.ZonedDateTime nextRun = now.withHour(8).withMinute(0).withSecond(0).withNano(0);
        if (now.isAfter(nextRun)) {nextRun = nextRun.plusDays(1);}
        long initialDelay = java.time.Duration.between(now, nextRun).getSeconds();
        System.out.println("[Scheduler] Current time: " + now);
        System.out.println("[Scheduler] Next run scheduled for: " + nextRun);
        System.out.println("[Scheduler] Initial delay: " + initialDelay + " seconds");

        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("[Scheduler] Starting daily subscription expiry check...");
                PurchaseRepository.getInstance().checkAndNotifyExpiringSubscriptions();
                System.out.println("[Scheduler] Daily check completed successfully.");
            } catch (Exception e) {
                System.err.println("[Scheduler] Error during daily check: " + e.getMessage());
                e.printStackTrace();
            }
        }, initialDelay, 24 * 60 * 60, TimeUnit.SECONDS);
    }
}
