package server;

import java.io.IOException;
import java.net.ServerSocket;

public class appServer {
    private static GcmServer gcmServer;
    public static void main(String[] args) throws IOException{
        gcmServer = new GcmServer(5555);
        System.out.println("Server Started and Listening on port 5555");
        gcmServer.listen();
    }
}
