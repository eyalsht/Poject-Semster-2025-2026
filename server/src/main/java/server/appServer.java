package server;

import server.handler.HandlerRegistry;
import server.report.ActivityStatsScheduler;

import java.io.IOException;

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

        // Start daily refresh at 23:59 using the central ReportManager
        ActivityStatsScheduler.start(HandlerRegistry.getInstance().getReportManager());
    }
}
