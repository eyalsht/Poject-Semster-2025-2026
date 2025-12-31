package server;

import ocsf.ocsf.server.AbstractServer;
import ocsf.ocsf.server.ConnectionToClient;
import entities.City;
import entities.actionType;
import entities.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GcmServer extends AbstractServer {

    public GcmServer(int port) {
        super(port);
    }
/*
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
*/
@Override
protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
    System.out.println("Message received: " + msg + " from " + client);

    if (msg instanceof Message) {
        Message request = (Message) msg;
        Message response = null; // הודעת התשובה שנשלח חזרה

        try {
            switch (request.getAction()) {

                case LOGIN_REQUEST:
                    System.out.println("Login request received");
                    break;

                case GET_ALL_CITIES_REQUEST:
                    System.out.println("Catalog request received");
                    ArrayList<City> cities = DBController.getAllCities();

                    response = new Message(actionType.GET_ALL_CITIES_RESPONSE, cities);
                    break;

                case UPDATE_PRICE_REQUEST:
                    System.out.println("Price update request received");
                    // [cityId, newPrice, type]
                    if (request.getMessage() instanceof ArrayList) {
                        ArrayList<Object> params = (ArrayList<Object>) request.getMessage();
                        int cityId = (int) params.get(0);
                        double newPrice = (double) params.get(1);
                        entities.PriceType type = (entities.PriceType) params.get(2);

                        boolean success = DBController.requestPriceUpdate(cityId, newPrice, type);

                        if (success) {
                            response = new Message(actionType.UPDATE_PRICE_SUCCESS, "Price update requested successfully");
                        }
                    }
                    break;

                // בתוך ה-switch(request.getAction())
                case GET_CITY_NAMES_REQUEST:
                    response = new Message(actionType.GET_CITY_NAMES_RESPONSE, DBController.getAllCityNames());
                    break;

                case GET_MAPS_REQUEST:
                    String cityForMaps = (String) request.getMessage();
                    response = new Message(actionType.GET_MAPS_RESPONSE, DBController.getMapNamesForCity(cityForMaps));
                    break;

                case GET_VERSIONS_REQUEST:
                    // הנחה: נשלחת רשימה [cityName, mapName]
                    List<String> versionParams = (List<String>) request.getMessage();
                    response = new Message(actionType.GET_VERSIONS_RESPONSE,
                            DBController.getVersionsForCityMap(versionParams.get(0), versionParams.get(1)));
                    break;

                case GET_CATALOG_REQUEST:
                    // הנחה: נשלחת רשימה [city, map, version]
                    List<String> catalogParams = (List<String>) request.getMessage();
                    response = new Message(actionType.GET_CATALOG_RESPONSE,
                            DBController.getCatalogRows(catalogParams.get(0), catalogParams.get(1), catalogParams.get(2)));
                    break;
                default:
                    System.out.println("Unknown action: " + request.getAction());
            }

            // שליחת התשובה חזרה ללקוח (רק אם נוצרה תשובה)
            if (response != null) {
                client.sendToClient(response);
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