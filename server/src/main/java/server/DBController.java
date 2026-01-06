package server;

import common.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBController {
    private static Connection connection;

    // -------------------- CONNECT --------------------
    public static void connectToDB(String dbUrl, String dbUser, String dbPassword) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("DB CONNECTED!");
        } catch (Exception e) {
            System.out.println("Error connecting to DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureConnected() {
        if (connection == null) throw new IllegalStateException("DB connection is null (connectToDB not called).");
    }

    // -------------------- USERS --------------------
    public static User getUserForLogin(String username) {
        ensureConnected();
        User user = null;

        String query = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String password = rs.getString("password");
                    String email = rs.getString("email");
                    String roleStr = rs.getString("user_role");
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
/*
                    if ("Client".equalsIgnoreCase(role)) {
                        user = new Client(id, username, password, email);
                    } else {
                        user = new Employee(id,firstName,lastName, username, password, email,password,UserRole.valueOf(roleStr), employeeId);
                    }

                    user.setFirstName(firstName);
                    user.setLastName(lastName);

                    user.setFailedAttempts(rs.getInt("failed_attempts"));
                    user.setBlocked(rs.getBoolean("is_blocked"));
                }*/

                    String employeeId = null;
                    try {
                        employeeId = rs.getString("employee_id");
                    } catch (SQLException e) {
                        // אם העמודה לא קיימת בטבלה, נשים ערך ברירת מחדל כדי שהקוד לא יקרוס
                        employeeId = "N/A";
                    }

                    if ("Client".equalsIgnoreCase(roleStr)) {
                        // אם זה לקוח - יצירה רגילה
                        user = new Client(id, username, password, email);
                        user.setFirstName(firstName);
                        user.setLastName(lastName);
                    } else {
                        // תיקון 3: שימוש במשתנים שהגדרנו כרגע (roleStr, employeeId)
                        // ושימוש בבנאי הנכון לפי הסדר: id, first, last, user, email, pass, role, empId
                        user = new Employee(
                                id,
                                firstName,
                                lastName,
                                username,
                                email,
                                password,
                                UserRole.valueOf(roleStr),
                                employeeId
                        );
                    }

                    if (user != null) {
                        user.setFailedAttempts(rs.getInt("failed_attempts"));
                        user.setBlocked(rs.getBoolean("is_blocked"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }

    public static void updateUserSecurityState(User user) {
        ensureConnected();
        String query = "UPDATE users SET failed_attempts = ?, is_blocked = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, user.getFailedAttempts());
            ps.setBoolean(2, user.isBlocked());
            ps.setInt(3, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean registerUser(String firstName, String lastName, String idNumber,
                                       String email, String password, String cardNumber) {
        ensureConnected();

        String checkSql = "SELECT COUNT(*) FROM users WHERE email = ? OR id_number = ?";
        String insertSql = """
            INSERT INTO users (username, password, email, user_role, first_name, last_name, id_number, credit_card, failed_attempts, is_blocked)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, false)
        """;

        try {
            String username = email;

            try (PreparedStatement check = connection.prepareStatement(checkSql)) {
                check.setString(1, email);
                check.setString(2, idNumber);

                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) return false;
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, email);
                ps.setString(4, "Client");
                ps.setString(5, firstName);
                ps.setString(6, lastName);
                ps.setString(7, idNumber);
                ps.setString(8, cardNumber);

                return ps.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // -------------------- CITIES / CATALOG --------------------
    public static City getCityByName(String cityName) {
        ensureConnected();
        City city = null;

        String query = "SELECT * FROM cities WHERE city_name = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, cityName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    city = new City(
                            rs.getInt("id"),
                            rs.getString("city_name"),
                            rs.getDouble("price_one_time"),
                            rs.getDouble("price_sub")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return city;
    }

    public static ArrayList<City> getAllCities() {
        ensureConnected();
        ArrayList<City> cities = new ArrayList<>();

        // IMPORTANT: your table uses city_name (not "name")
        String query = "SELECT id, city_name, price_one_time, price_sub FROM cities ORDER BY city_name";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                cities.add(new City(
                        rs.getInt("id"),
                        rs.getString("city_name"),
                        rs.getDouble("price_one_time"),
                        rs.getDouble("price_sub")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cities;
    }

    public static List<String> getAllCityNames() {
        ensureConnected();
        List<String> cities = new ArrayList<>();
        String sql = "SELECT city_name FROM cities ORDER BY city_name";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) cities.add(rs.getString("city_name"));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cities;
    }

    public static List<String> getMapNamesForCity(String cityName) {
        ensureConnected();
        List<String> maps = new ArrayList<>();

        String sql = """
            SELECT DISTINCT m.map_name
            FROM maps m
            JOIN cities c ON c.id = m.city_id
            WHERE c.city_name = ?
            ORDER BY m.map_name
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cityName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) maps.add(rs.getString("map_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return maps;
    }

    public static List<String> getVersionsForCityMap(String cityName, String mapName) {
        ensureConnected();
        List<String> versions = new ArrayList<>();

        String sql = """
            SELECT DISTINCT m.version
            FROM maps m
            JOIN cities c ON c.id = m.city_id
            WHERE c.city_name = ? AND m.map_name = ?
            ORDER BY m.version
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cityName);
            ps.setString(2, mapName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) versions.add(rs.getString("version"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return versions;
    }

    public static List<MapCatalogRow> getCatalogRows(String cityName, String mapName, String version) {
        ensureConnected();
        List<MapCatalogRow> rows = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT 
                c.city_name AS city,
                m.map_name  AS map,
                m.version   AS version,
                m.price     AS price,
                m.description AS description
            FROM maps m
            JOIN cities c ON c.id = m.city_id
            WHERE 1=1
        """);

        if (cityName != null && !cityName.isBlank()) sql.append(" AND c.city_name = ? ");
        if (mapName != null && !mapName.isBlank()) sql.append(" AND m.map_name = ? ");
        if (version != null && !version.isBlank()) sql.append(" AND m.version = ? ");
        sql.append(" ORDER BY c.city_name, m.map_name, m.version ");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            if (cityName != null && !cityName.isBlank()) ps.setString(idx++, cityName);
            if (mapName != null && !mapName.isBlank()) ps.setString(idx++, mapName);
            if (version != null && !version.isBlank()) ps.setString(idx++, version);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new MapCatalogRow(
                            rs.getString("city"),
                            rs.getString("map"),
                            rs.getString("version"),
                            rs.getDouble("price"),
                            rs.getString("description")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    // -------------------- MAP ID --------------------
    public static Integer getMapId(String cityName, String mapName, String version) {
        ensureConnected();

        String sql = """
            SELECT m.id
            FROM maps m
            JOIN cities c ON c.id = m.city_id
            WHERE c.city_name = ? AND m.map_name = ? AND m.version = ?
            LIMIT 1
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cityName);
            ps.setString(2, mapName);
            ps.setString(3, version);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // -------------------- PENDING MAP PRICE UPDATES (TABLE: pending_price_updates) --------------------
    public static boolean createPendingMapPriceUpdate(int mapId, double newPrice, Integer requesterUserId) {
        ensureConnected();

        String getOld = "SELECT price FROM maps WHERE id = ?";
        String insert = """
            INSERT INTO pending_price_updates(map_id, requester_user_id, old_price, new_price)
            VALUES (?, ?, ?, ?)
        """;

        try {
            double oldPrice;

            try (PreparedStatement ps = connection.prepareStatement(getOld)) {
                ps.setInt(1, mapId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    oldPrice = rs.getDouble("price");
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(insert)) {
                ps.setInt(1, mapId);

                if (requesterUserId == null) ps.setNull(2, Types.INTEGER);
                else ps.setInt(2, requesterUserId);

                ps.setDouble(3, oldPrice);
                ps.setDouble(4, newPrice);

                return ps.executeUpdate() > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<PendingPriceUpdate> getPendingMapPriceUpdates() {
        ensureConnected();

        List<PendingPriceUpdate> list = new ArrayList<>();

        String sql = """
        SELECT p.id, p.map_id, c.city_name, m.map_name, m.version,
               p.old_price, p.new_price, p.created_at
        FROM pending_price_updates p
        JOIN maps m ON m.id = p.map_id
        JOIN cities c ON c.id = m.city_id
        WHERE p.status = 'PENDING'
        ORDER BY p.created_at DESC
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new PendingPriceUpdate(
                        rs.getInt("id"),
                        rs.getInt("map_id"),
                        rs.getString("city_name"),
                        rs.getString("map_name"),
                        rs.getString("version"),
                        rs.getDouble("old_price"),
                        rs.getDouble("new_price"),
                        rs.getTimestamp("created_at")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static boolean approvePendingMapPriceUpdate(int pendingId, Integer reviewerIdIgnored) {
        ensureConnected();

        String get = "SELECT map_id, new_price FROM pending_price_updates WHERE id = ? AND status='PENDING'";
        String updMap = "UPDATE maps SET price = ? WHERE id = ?";
        String del = "DELETE FROM pending_price_updates WHERE id = ?";

        try {
            connection.setAutoCommit(false);

            int mapId;
            double newPrice;

            try (PreparedStatement ps = connection.prepareStatement(get)) {
                ps.setInt(1, pendingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return false;
                    }
                    mapId = rs.getInt("map_id");
                    newPrice = rs.getDouble("new_price");
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(updMap)) {
                ps.setDouble(1, newPrice);
                ps.setInt(2, mapId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(del)) {
                ps.setInt(1, pendingId);
                ps.executeUpdate();
            }

            connection.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try { connection.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }


    public static boolean denyPendingMapPriceUpdate(int pendingId, Integer reviewerIdIgnored) {
        ensureConnected();

        String del = "DELETE FROM pending_price_updates WHERE id = ? AND status='PENDING'";

        try (PreparedStatement ps = connection.prepareStatement(del)) {
            ps.setInt(1, pendingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getPendingPriceUpdatesCount() {
        ensureConnected();

        String sql = "SELECT COUNT(*) FROM pending_price_updates WHERE status='PENDING'";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


}
