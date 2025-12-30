package server;

import java.io.IOException;
import java.util.Scanner;

public class ServerUI {
    final public static int PORT=5555;
    public static void main(String[] args) throws IOException {
        int port=0;
        try {
            port=Integer.parseInt((args[0]));

        }catch (Throwable t){
            port= PORT;
        }

        GcmServer server = new GcmServer(port);

        try{
            server.listen();
        }
        catch (Exception e){
            System.err.println("Server could not be started.");
        }

        System.out.println("Server started on port " + port);
        System.out.println("Type 'Exit' to exit or 'quit' to quit.");
        Scanner sc = new Scanner(System.in);
        while(true){
            String s = sc.nextLine();
            if(s.equals("Exit")||s.equals("quit")){
                try{
                    server.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
                System.out.println(("server stopped."));
                break;
            }
        }


    }
}
