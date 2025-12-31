package server;

import ocsf.ocsf.server.AbstractServer;
import ocsf.ocsf.server.ConnectionToClient;
import entities.City;
import entities.actionType;
import entities.Message;
import java.io.IOException;
import java.util.Properties;

public class GcmServer extends AbstractServer {

    public GcmServer(int port) {
        super(port);
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        System.out.println("Message received: " + msg + " from " + client);

        if (msg instanceof Message) {
            Message request = (Message) msg;

            try {
                switch (request.getAction()) {

                    case LOGIN_REQUEST:
                        System.out.println("Login request received");
                        break;

                    case GET_ALL_CITIES_REQUEST:
                        System.out.println("Catalog request received");
                        break;

                    case UPDATE_PRICE_REQUEST:
                        System.out.println("Price update request received");
                        break;

                    default:
                        System.out.println("Unknown action: " + request.getAction());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Warning: Received a non-Message object!");
        }
    }

    @Override
    protected void serverStarted() {
        System.out.println("Server listening for connections on port " + getPort());

        try {
            Properties props = new Properties();

            // Load from classpath (src/main/resources)
            try (var in = GcmServer.class.getResourceAsStream("/db.properties")) {
                if (in == null) {
                    System.err.println("ERROR: db.properties not found in src/main/resources");
                    return;
                }
                props.load(in);
            }

            String dbUrl = props.getProperty("db.url");
            String dbUser = props.getProperty("db.user");
            String dbPassword = props.getProperty("db.password");

            DBController.connectToDB(dbUrl, dbUser, dbPassword);

        } catch (Exception e) {
            System.err.println("Error loading database configuration:");
            e.printStackTrace();
        }
    }


    @Override
    protected void serverStopped() {
        System.out.println("Server has stopped listening for connections.");
    }
}