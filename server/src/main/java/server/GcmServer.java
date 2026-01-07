package server;

import common.*;

import server.ocsf.AbstractServer;
import server.ocsf.ConnectionToClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GcmServer extends AbstractServer {

    public GcmServer(int port) {
        super(port);
    }

    private static final int MAX_ATTEMPTS = 3;
    private static final int BLOCK_TIME_SEC = 30;

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        System.out.println("Message received: " + msg + " from " + client);

        if (!(msg instanceof Message)) {
            System.out.println("Warning: Received a non-Message object!");
            return;
        }

        Message request = (Message) msg;
        Message response = null;

        try {
            switch (request.getAction()) {

                case LOGIN_REQUEST: {
                    try {
                        ArrayList<String> creds = (ArrayList<String>) request.getMessage();
                        String username = creds.get(0);
                        String password = creds.get(1);

                        User user = DBController.getUserForLogin(username);

                        if (user == null) {
                            response = new Message(actionType.LOGIN_FAILED, "User does not exist.");
                            break;
                        }

                        synchronized (user) {

                            if (user.isBlocked()) {
                                response = new Message(actionType.LOGIN_FAILED, "User is BLOCKED.");
                                break;
                            }

                            if (user.getPassword().equals(password)) {
                                user.resetFailedAttempts();
                                DBController.updateUserSecurityState(user);

                                response = new Message(actionType.LOGIN_SUCCESS, user);

                            } else {
                                user.incrementFailedAttempts();

                                if (user.getFailedAttempts() >= MAX_ATTEMPTS) {
                                    user.setBlocked(true);
                                    DBController.updateUserSecurityState(user);

                                    response = new Message(actionType.LOGIN_FAILED, "Blocked for " + BLOCK_TIME_SEC + "s.");

                                    new Thread(() -> {
                                        try {
                                            Thread.sleep(BLOCK_TIME_SEC * 1000);

                                            synchronized (user) {
                                                user.setBlocked(false);
                                                user.resetFailedAttempts();
                                                DBController.updateUserSecurityState(user);
                                                System.out.println("User " + user.getUsername() + " unblocked automatically.");
                                            }
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }).start();

                                } else {
                                    DBController.updateUserSecurityState(user);
                                    response = new Message(
                                            actionType.LOGIN_FAILED,
                                            "Wrong password. Attempt " + user.getFailedAttempts() + "/" + MAX_ATTEMPTS
                                    );
                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        response = new Message(actionType.LOGIN_FAILED, "Server error during login");
                    }
                    break;
                }

                case GET_ALL_CITIES_REQUEST: {
                    System.out.println("Catalog request received");
                    ArrayList<City> cities = DBController.getAllCities();
                    response = new Message(actionType.GET_ALL_CITIES_RESPONSE, cities);
                    break;
                }

                case GET_CITY_NAMES_REQUEST: {
                    response = new Message(actionType.GET_CITY_NAMES_RESPONSE, DBController.getAllCityNames());
                    break;
                }

                case GET_MAPS_REQUEST: {
                    String cityForMaps = (String) request.getMessage();
                    response = new Message(actionType.GET_MAPS_RESPONSE, DBController.getMapNamesForCity(cityForMaps));
                    break;
                }

                case GET_VERSIONS_REQUEST: {
                    List<String> versionParams = (List<String>) request.getMessage();
                    response = new Message(
                            actionType.GET_VERSIONS_RESPONSE,
                            DBController.getVersionsForCityMap(versionParams.get(0), versionParams.get(1))
                    );
                    break;
                }

                case GET_CATALOG_REQUEST: {
                    System.out.println("CATALOG REQUEST → querying DB");
                    List<String> catalogParams = (List<String>) request.getMessage();

                    // 1. Print params to ensure they are what you expect (should be empty strings, not nulls)
                    System.out.println("Params received: " + catalogParams);

                    // 2. Fetch data into a variable first
                    List<MapCatalogRow> results = DBController.getCatalogRows(
                            catalogParams.get(0),
                            catalogParams.get(1),
                            catalogParams.get(2)
                    );

                    // 3. Print the size of the result list
                    if (results == null) {
                        System.out.println("ERROR: DBController returned NULL!");
                    } else {
                        System.out.println("DB Success! Found " + results.size() + " rows.");
                    }

                    response = new Message(actionType.GET_CATALOG_RESPONSE, results);
                    break;
                }

                case REGISTER_REQUEST: {
                    try {
                        ArrayList<String> data = (ArrayList<String>) request.getMessage();

                        String firstName = data.get(0);
                        String lastName  = data.get(1);
                        String idNumber  = data.get(2);
                        String email     = data.get(3);
                        String password  = data.get(4);
                        String card      = data.get(5);

                        boolean ok = DBController.registerUser(firstName, lastName, idNumber, email, password, card);

                        if (ok) {
                            response = new Message(actionType.REGISTER_SUCCESS, "Registered successfully");
                        } else {
                            response = new Message(actionType.REGISTER_FAILED, "User already exists (email or id)");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        response = new Message(actionType.REGISTER_FAILED, "Server error during register");
                    }
                    break;
                }

                // ===================== FIXED: price approvals flow (NO DUPLICATES) =====================

                case GET_PENDING_PRICE_APPROVALS_REQUEST: {
                    response = new Message(
                            actionType.GET_PENDING_PRICE_APPROVALS_RESPONSE,
                            DBController.getPendingMapPriceUpdates()
                    );
                    break;
                }

                case UPDATE_PRICE_REQUEST: {
                    // MAP price update request
                    // Expect: [cityName, mapName, version, newPrice, requesterId]
                    ArrayList<Object> p = (ArrayList<Object>) request.getMessage();

                    String cityName = (String) p.get(0);
                    String mapName  = (String) p.get(1);
                    String version  = (String) p.get(2);
                    double newPrice = (double) p.get(3);
                    Integer requesterId = (Integer) p.get(4);

                    Integer mapId = DBController.getMapId(cityName, mapName, version);
                    boolean ok = (mapId != null)
                            && DBController.createPendingMapPriceUpdate(mapId, newPrice, requesterId);

                    response = new Message(actionType.UPDATE_PRICE_SUCCESS, ok);
                    break;
                }

                case APPROVE_PENDING_PRICE_REQUEST: {
                    // Expect: [pendingId, reviewerId]
                    ArrayList<Object> p = (ArrayList<Object>) request.getMessage();
                    int pendingId = (int) p.get(0);
                    Integer reviewerId = (Integer) p.get(1);

                    boolean ok = DBController.approvePendingMapPriceUpdate(pendingId, reviewerId);
                    response = new Message(actionType.APPROVE_PRICE_SUCCESS, ok);
                    break;
                }

                case DENY_PENDING_PRICE_REQUEST: {
                    // Expect: [pendingId, reviewerId]
                    ArrayList<Object> p = (ArrayList<Object>) request.getMessage();
                    int pendingId = (int) p.get(0);
                    Integer reviewerId = (Integer) p.get(1);

                    boolean ok = DBController.denyPendingMapPriceUpdate(pendingId, reviewerId);
                    response = new Message(actionType.DENY_PRICE_SUCCESS, ok);
                    break;
                }
                case GET_PENDING_PRICE_APPROVALS_COUNT_REQUEST: {
                    int count = DBController.getPendingPriceUpdatesCount();
                    response = new Message(
                            actionType.GET_PENDING_PRICE_APPROVALS_COUNT_RESPONSE,
                            count
                    );
                    break;
                }


                // ================================================================================

                default: {
                    System.out.println("Unknown action: " + request.getAction());
                    break;
                }
            }

            if (response != null) {
                client.sendToClient(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void serverStarted() {
        System.out.println("Server listening for connections on port " + getPort());

        try {
            Properties props = new Properties();

            // Load from classpath (server/src/main/resources/db.properties)
            try (var in = GcmServer.class.getResourceAsStream("/db.properties")) {
                if (in == null) {
                    System.err.println("ERROR: db.properties not found in server/src/resources/db.properties");
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
