package server;

import entities.User;
import entities.City;
import java.sql.*;

public class DBController {
    private static Connection connection;

    public static void connectToDatabase(String dbUrl, String dbUser, String dbPassword) throws SQLException, ClassNotFoundException {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("Connected to the Database");
        }
        catch(Exception e){
            System.out.println("Error connecting to DB: "+e.getMessage());
        }
    }
    public static City getCity(String cityName) throws SQLException, ClassNotFoundException {
        try{
            City city =null;
            String query = "SELECT * FROM city WHERE name = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, cityName);
            if (rs.next()){
                city = new City(
                rs.getInt("id"),
                rs.GetString("city_name"),
                rs.GetDouble("price_one_time"),
                rs.GetDouble("price_sub")
                );
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        return city;
    }
}
