package server;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import entities.City;
import java.io.IOException;

public class GcmServer extends AbstractServer {

    public GcmServer(int port) {
        super(port);
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
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

        // כשהשרת מתחיל, אנחנו מחברים אותו ל-DB
        // שים לב: שנה את הסיסמה לסיסמה שלך ב-MySQL
        DBController.connectToDB("jdbc:mysql://localhost/gcm_db?serverTimezone=IST", "root", "123456");
    }

    @Override
    protected void serverStopped() {
        System.out.println("Server has stopped listening for connections.");
    }
}