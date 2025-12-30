package server;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import entities.City;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;

public class GcmServer extends AbstractServer {

    public GcmServer(int port) {
        super(port);
    }


    protected  void handleMessageFromClient(Object msg, ConnectionToClient client) {
        System.out.println("Message received: " + msg + " from " + client);

        // נניח שהלקוח שולח מחרוזת פשוטה שהיא שם העיר (בפרויקט אמיתי שולחים אובייקט בקשה מסודר)
        // אבל לצורך הדוגמה של הקישור:
        if (msg instanceof String) {
            String request = (String) msg;

            // בדיקה האם הבקשה היא לקבלת עיר
            // פרוטוקול דוגמה: "get_city:Tel Aviv"
            if (request.startsWith("get_city:")) {
                String cityName = request.split(":")[1];

                // >>> כאן מתבצע הקישור ל-DBController! <<<
                City city = DBController.getCityByName(cityName);

                try {
                    // שליחת האובייקט חזרה ללקוח
                    client.sendToClient(city);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void serverStarted() {
        System.out.println("Server listening for connections on port " + getPort());
        
        try {
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream("db.properties");
            props.load(fis);
            fis.close();
            
            String dbUrl = props.getProperty("db.url");
            String dbUser = props.getProperty("db.user");
            String dbPassword = props.getProperty("db.password");
            
            // כשהשרת מתחיל, אנחנו מחברים אותו ל-DB
            DBController.connectToDB(dbUrl, dbUser, dbPassword);
        } catch (IOException e) {
            System.err.println("Error loading database configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void serverStopped() {
        System.out.println("Server has stopped listening for connections.");
    }
}