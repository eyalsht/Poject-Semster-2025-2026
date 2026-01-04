package server;

import common.*;

import java.sql.*;

import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBController {
    private static Connection connection;

    public static void connectToDB(String dbUrl, String dbUser, String dbPassword) {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("DB CONNECTED!");
            System.out.println("DB URL = " + dbUrl);
            System.out.println("DB USER = " + dbUser);
            System.out.println("CONNECTION OBJECT = " + connection);
        }
        catch(Exception e){
            System.out.println("Error connecting to DB: "+e.getMessage());
        }
    }
    public static City getCityByName(String cityName) {
        City city = null;
        String query = "SELECT * FROM cities WHERE city_name = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, cityName);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()){
                city = new City(
                        rs.getInt("id"),
                        rs.getString("city_name"),
                        rs.getDouble("price_one_time"),
                        rs.getDouble("price_sub")
                );
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return city;
    }
    public static List<MapCatalogRow> getCatalogRows(String cityName, String mapName, String version)
    {
        System.out.println("DBController.getCatalogRows CALLED");
        List<MapCatalogRow> rows = new ArrayList<>();

        // Build a flexible query: if user selected only city, or city+map, etc.
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
    public static List<String> getAllCityNames() {
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
    public static ArrayList<City> getAllCities() {
        ArrayList<City> cities = new ArrayList<>();
        String query = "SELECT * FROM cities";

        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs =ps.executeQuery();

            while (rs.next()){
                int id=rs.getInt("id");
                String name=rs.getString("name");
                double priceOneTime=rs.getDouble("price_one_time");
                double PriceSub=rs.getDouble("price_sub");

                City city = new City(id,name,priceOneTime,PriceSub);
                cities.add(city);
            }
            rs.close();
            ps.close();
        }
        catch(SQLException e) {
            System.out.println("Error fetching cities: " + e.getMessage());
            e.printStackTrace();
        }
        return cities;
    }
    public static boolean requestPriceUpdate(int cityId, double newPrice, PriceType type) {
        String column = (type == PriceType.ONE_TIME) ? "pending_price_one_time" : "pending_price_sub";
        String query = "UPDATE cities SET " + column + " = ? WHERE id = ?";

        try{
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setDouble(1, newPrice);
            ps.setInt(2, cityId);

            int rows=ps.executeUpdate();
            ps.close();
            return rows > 0;

        }
        catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }
    public static boolean approvePriceUpdate(int cityId, PriceType type) {
        String pendingCol = (type == PriceType.ONE_TIME) ? "pending_price_one_time" : "pending_price_sub";
        String realCol = (type == PriceType.ONE_TIME) ? "price_one_time" : "price_sub";

        String query = "UPDATE cities SET " + realCol + " = " + pendingCol + ", " + pendingCol + " = -1 WHERE id = ?";

        try{
            PreparedStatement ps= connection.prepareStatement(query);
            ps.setInt(1,cityId);

            int rows=ps.executeUpdate();
            ps.close();
            return rows > 0;
        }
        catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }
    public static User getUserForLogin(String username) {
        User user = null;

        if (connection == null) {
            System.out.println("Error: Database connection is null!");
            return null;
        }

        String query = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    int id = rs.getInt("id");
                    String password = rs.getString("password");
                    String email = rs.getString("email");
                    String role = rs.getString("user_role");

                    String firstName = rs.getString("first_name");
                    String lastName  = rs.getString("last_name");

                    if ("Client".equalsIgnoreCase(role)) {
                        user = new Client(id, username, password, email);
                    } else {
                        user = new Employee(id, username, password, email, role);
                    }

                    if (user != null) {
                        user.setFirstName(firstName);
                        user.setLastName(lastName);

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
        String query = "UPDATE users SET failed_attempts = ?, is_blocked = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, user.getFailedAttempts());
            ps.setBoolean(2, user.isBlocked());
            ps.setInt(3, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
    public static boolean registerUser(String firstName, String lastName, String idNumber,
                                       String email, String password, String cardNumber) {

        // 1) Check duplicates (email OR id)
        String checkSql = "SELECT COUNT(*) FROM users WHERE email = ? OR id_number = ?";
        String insertSql = """
        INSERT INTO users (username, password, email, user_role, first_name, last_name, id_number, credit_card, failed_attempts, is_blocked)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, false)
    """;

        try {
            // make username = email for now (simple)
            String username = email;

            try (PreparedStatement check = connection.prepareStatement(checkSql)) {
                check.setString(1, email);
                check.setString(2, idNumber);

                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false; // already exists
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, email);
                ps.setString(4, "Client"); // default role for new register
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

}
