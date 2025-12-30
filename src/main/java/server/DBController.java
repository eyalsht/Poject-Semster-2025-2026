package main.java.server;

import entities.User;
import entities.City;
import java.sql.*;

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
}
