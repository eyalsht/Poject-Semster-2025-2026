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
            t.setDaemon(true); // הופך את ה-Thread ל-Daemon כדי שלא ימנע מהשרת להיסגר
            return t;
        });

        // הרצה ראשונה אחרי דקה, ואז כל 24 שעות
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("[Scheduler] Starting daily subscription expiry check...");
                PurchaseRepository.getInstance().checkAndNotifyExpiringSubscriptions();
                System.out.println("[Scheduler] Daily check completed successfully.");
            } catch (Exception e) {
                System.err.println("[Scheduler] Error during daily check: " + e.getMessage());
                e.printStackTrace();
            }
        }, 1, 24, TimeUnit.HOURS);
    }
}
