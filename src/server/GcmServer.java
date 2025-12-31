package server;

import ocsf.ocsf.server.AbstractServer;
import ocsf.ocsf.server.ConnectionToClient;
import entities.City;
import java.io.IOException;
import java.util.Properties;

public class GcmServer extends AbstractServer {

    public GcmServer(int port) {
        super(port);
    }


    /*protected  void handleMessageFromClient(Object msg, ConnectionToClient client) {
        System.out.println("Message received: " + msg + " from " + client);

        // נניח שהלקוח שולח מחרוזת פשוטה שהיא שם העיר (בפרויקט אמיתי שולחים אובייקט בקשה מסודר)
        // אבל לצורך הדוגמה של הקישור:
        if (msg instanceof String) {
            String request = (String) msg;

            // בדיקה האם הבקשה היא לקבלת עיר
            // פרוטוקול דוגמה: "get_city:Tel Aviv"
            if (request.startsWith("get_city:")) {
                String cityName = request.split(":")[1];

                // >>> כאן מתבצע הקישור ל-server.DBController! <<<
                City city = DBController.getCityByName(cityName);

                try {
                    // שליחת האובייקט חזרה ללקוח
                    client.sendToClient(city);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if (!(msg instanceof String request)) {
            System.out.println("SERVER got non-string msg: " + msg);
            return;
        }

        System.out.println("SERVER got request: " + request);

        try {
            if (request.equals("get_cities")) {
                var cities = DBController.getAllCityNames();
                System.out.println("SERVER sending cities: " + cities.size());
                client.sendToClient(cities);
                return;
            }

            if (request.startsWith("get_maps:")) {
                String city = request.split(":", 2)[1];
                var maps = DBController.getMapNamesForCity(city);
                System.out.println("SERVER sending maps for " + city + ": " + maps.size());
                client.sendToClient(maps);
                return;
            }

            if (request.startsWith("get_versions:")) {
                String[] parts = request.split(":", 3);
                String city = parts[1];
                String map  = parts[2];
                var versions = DBController.getVersionsForCityMap(city, map);
                System.out.println("SERVER sending versions for " + city + "/" + map + ": " + versions.size());
                client.sendToClient(versions);
                return;
            }

            if (request.startsWith("get_catalog:")) {
                String[] parts = request.split(":", 4);
                String city = parts.length > 1 && !parts[1].isBlank() ? parts[1] : null;
                String map  = parts.length > 2 && !parts[2].isBlank() ? parts[2] : null;
                String ver  = parts.length > 3 && !parts[3].isBlank() ? parts[3] : null;

                var rows = DBController.getCatalogRows(city, map, ver);
                System.out.println("SERVER sending catalog rows: " + rows.size()
                        + " first=" + (rows.isEmpty() ? "none" : rows.get(0).getClass()));
                client.sendToClient(rows);
                return;
            }

            System.out.println("SERVER unknown request: " + request);

        } catch (Exception e) {
            e.printStackTrace();
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