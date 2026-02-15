package server.handler;

import common.enums.ActionType;
import common.enums.MapStatus;
import common.messaging.Message;
import org.hibernate.Session;
import org.hibernate.Transaction;
import server.HibernateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Temporary handler for adding maps to the simulated external repository.
 * Receives a List containing: [mapName, description, cityName, imageBytes].
 * Uses raw JDBC via session.doWork() to avoid all Hibernate eager-loading issues.
 */
public class AddExternalMapHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        Transaction tx = null;
        Session session = null;
        try {
            @SuppressWarnings("unchecked")
            List<Object> params = (List<Object>) request.getMessage();

            String mapName = (String) params.get(0);
            String description = (String) params.get(1);
            String cityName = (String) params.get(2);
            byte[] imageBytes = (byte[]) params.get(3);

            System.out.println("AddExternalMap: received request - map=" + mapName
                    + ", city=" + cityName
                    + ", imageSize=" + (imageBytes != null ? imageBytes.length : "null"));

            if (mapName == null || mapName.isBlank() || cityName == null || cityName.isBlank()) {
                System.err.println("AddExternalMap: mapName or cityName is blank");
                return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
            }

            if (imageBytes == null || imageBytes.length == 0) {
                System.err.println("AddExternalMap: imageBytes is null or empty");
                return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
            }

            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Use raw JDBC to completely bypass Hibernate entity loading
            final int[] cityIdHolder = new int[1];
            final boolean[] success = {false};

            session.doWork((Connection connection) -> {
                // Step 1: Find city by name
                int cityId = -1;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT id FROM cities WHERE name = ?")) {
                    ps.setString(1, cityName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            cityId = rs.getInt(1);
                            System.out.println("AddExternalMap: found city '" + cityName + "' id=" + cityId);
                        }
                    }
                }

                // Step 2: Create city if not found
                if (cityId == -1) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO cities (name, description, price_sub) VALUES (?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, cityName);
                        ps.setString(2, "Auto-created for external map: " + mapName);
                        ps.setDouble(3, 0.0);
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next()) {
                                cityId = keys.getInt(1);
                                System.out.println("AddExternalMap: created city '" + cityName + "' id=" + cityId);
                            }
                        }
                    }
                }

                // Step 3: Insert the map
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO maps (name, description, version, status, price, city_id, map_image) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, mapName);
                    ps.setString(2, description != null ? description : "");
                    ps.setString(3, "1.0");
                    ps.setString(4, MapStatus.EXTERNAL.name());
                    ps.setDouble(5, 0.0);
                    ps.setInt(6, cityId);
                    ps.setBytes(7, imageBytes);
                    ps.executeUpdate();
                    System.out.println("AddExternalMap: inserted map '" + mapName + "' for city id=" + cityId);
                }

                cityIdHolder[0] = cityId;
                success[0] = true;
            });

            tx.commit();
            session.close();

            if (success[0]) {
                System.out.println("Added external map: " + mapName + " in city: " + cityName);
                return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, true);
            } else {
                System.err.println("AddExternalMap: doWork completed but success flag not set");
                return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
            }

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            if (session != null && session.isOpen()) session.close();
            System.err.println("AddExternalMap FAILED: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
        }
    }
}
