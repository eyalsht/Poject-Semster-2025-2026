package client;

import java.io.IOException;
import common.User;
import oscf.AbstractClient;

public class GCMClient extends AbstractClient {

    // Singleton instance
    private static GCMClient instance;

    // Fields for handling responses (based on your diagram)
    private boolean busy = false;        // only 1 request at a time
    private boolean awaitResponse = false;
    private Object lastResponse = null;;

    /**
     * Private constructor implements the Singleton pattern.
     * Connects to the server immediately upon creation.
     */
    private GCMClient(String host, int port) throws IOException {
        super(host, port);
        openConnection();
        System.out.println("GCMClient connected to server on " + host + ":" + port);
    }

    /**
     * Returns the single instance of the client.
     * If it doesn't exist, it creates it.
     */
    public static GCMClient getInstance() {
        if (instance == null) {
            try {
                instance = new GCMClient("20.250.162.225", 5555);
            } catch (IOException e) {
                System.err.println("WARNING: Failed to connect to server. Make sure the server is running on 20.250.162.225:5555");
                e.printStackTrace();
                // Return null instance - caller should check connection status
            }
        }
        if(instance!=null) {
            return instance;
        }
        return null;
    }
    

    /**
     * Handles the message received from the server.
     * Signals the waiting thread that a response has arrived.
     */
    @Override
    protected void handleMessageFromServer(Object msg) {
        System.out.println("GCMClient received: " + msg);

        synchronized (this) {
            this.lastResponse = msg;
            this.awaitResponse = false;
            this.notifyAll();
        }
    }

    /**
     * Sends a request to the server and waits for a response.
     * This blocks the calling thread until the server replies.
     * * @param msg The message/request object to send
     * @return The object returned by the server
     */
    public Object sendRequest(Object msg) {
        synchronized (this) {

            // wait until no other request is running
            while (busy) {
                try { wait(); } catch (InterruptedException e) { e.printStackTrace(); }
            }
            busy = true;

            // prepare waiting for THIS response
            awaitResponse = true;
            lastResponse = null;

            try {
                sendToServer(msg);

                while (awaitResponse) {
                    try { wait(); } catch (InterruptedException e) { e.printStackTrace(); }
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Could not send message to server.");
            } finally {
                busy = false;
                notifyAll(); // allow next request to run
            }

            return lastResponse;
        }
    }


    // Optional: Methods to manage connection explicitly if needed
    public void quit() {
        try {
            closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // holds the logged-in user (session)
    private volatile User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

}