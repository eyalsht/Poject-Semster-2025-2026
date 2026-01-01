package server;

import main.java.common.*;

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
            System.out.println("Connected to the Database");
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
    public static List<MapCatalogRow> getCatalogRows(String cityName, String mapName, String version) {
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
        String query = "UPDATE cities SET " + column + " = ? " + column + " WHERE id = ?";

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
    public static User getUserForLogin(String username)
    {
        User user = null;
        String query = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    int id = rs.getInt("id");
                    String password = rs.getString("password");
                    String email = rs.getString("email");


                    if ("Client".equalsIgnoreCase(role)) {
                        user = new Client(id, username, password, email);
                    } else if ("Company_Manager".equalsIgnoreCase(role) || "Content_Manager".equalsIgnoreCase(role) || "Content_Worker".equalsIgnoreCase(role) || "Support_Agent".equalsIgnoreCase(role)) {
                        user = new Employee(id, username, password, email, role /* פרמטרים נוספים */);
                    }
                    else {
                        System.out.println("DEBUG: Role [" + role + "] does not match Client/Employee/Manager");
                    }


                    if (user != null) {
                        user.setFailedAttempts(rs.getInt("failed_attempts"));
                        user.setBlocked(rs.getBoolean("is_blocked"));
                    }
                }
            }
        }
        catch (SQLException e)
            { e.printStackTrace();
        }
        return user;
    }

    // פונקציה לעדכון מצב המשתמש ב-DB (חסימה/איפוס)
    public static void updateUserSecurityState(User user) {
        String query = "UPDATE users SET failed_attempts = ?, is_blocked = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, user.getFailedAttempts());
            ps.setBoolean(2, user.isBlocked());
            ps.setInt(3, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
