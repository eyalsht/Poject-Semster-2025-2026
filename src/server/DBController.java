package server;

import entities.City;
import java.sql.*;
import entities.MapCatalogRow;
import java.util.ArrayList;
import java.util.List;

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


}
