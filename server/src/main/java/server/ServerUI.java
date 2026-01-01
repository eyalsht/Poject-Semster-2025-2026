package server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

public class ServerUI {
    final public static int PORT = 5555;

    public static void main(String[] args) throws IOException {
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Throwable t) {
            port = PORT;
        }

        // 1) Load DB properties
        Properties p = new Properties();
        try (InputStream in = ServerUI.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                System.err.println("ERROR: db.properties not found in src/main/resources");
                return;
            }
            p.load(in);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to load db.properties");
            e.printStackTrace();
            return;
        }

        String url = p.getProperty("db.url");
        String user = p.getProperty("db.user");
        String pass = p.getProperty("db.password");

        // 2) Connect DB BEFORE starting server
        DBController.connectToDB(url, user, pass);

        // Optional quick test (recommended)
        // DBController.testQuery();  // we can add this method if you want

        // 3) Start server
        GcmServer server = new GcmServer(port);

        try {
            server.listen();
        } catch (Exception e) {
            System.err.println("Server could not be started.");
            e.printStackTrace();
            return;
        }

        System.out.println("Server started on port " + port);
        System.out.println("Type 'Exit' to exit or 'quit' to quit.");

        Scanner sc = new Scanner(System.in);
        while (true) {
            String s = sc.nextLine();
            if (s.equals("Exit") || s.equals("quit")) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("server stopped.");
                break;
            }
        }
    }
}
