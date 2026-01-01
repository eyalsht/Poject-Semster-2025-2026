package server;

public class ServerLauncher {

    private static GcmServer server;

    public static void startServer(int port) {
        if (server != null) return;

        Thread t = new Thread(() -> {
            try {
                server = new GcmServer(port);
                server.listen(); // starts server thread
                System.out.println("Server auto-started on port " + port);
            } catch (Exception e) {
                System.err.println("Failed to start server:");
                e.printStackTrace();
            }
        }, "GCM-Server-Thread");

        t.setDaemon(true); // JVM can exit when UI closes
        t.start();
    }

    public static void stopServer() {
        try {
            if (server != null) {
                server.close();
                System.out.println("Server stopped");
                server = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
