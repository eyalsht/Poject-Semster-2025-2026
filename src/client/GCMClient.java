package client;

import ocsf.ocsf.client.*;
import java.io.IOException;

public class GCMClient extends AbstractClient {

    // Singleton instance
    private static GCMClient instance;

    // Fields for handling responses (based on your diagram)
    private boolean awaitResponse = false;
    private Object lastResponse = null;

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
                // Default connection details - typically localhost/5555 for development
                instance = new GCMClient("localhost", 5555);
            } catch (IOException e) {
                e.printStackTrace();
                // You might want to show an Alert here in a real app
            }
        }
        return instance;
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
            this.notifyAll(); // Wake up the thread waiting in sendRequest
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
            // 1. Reset state
            awaitResponse = true;
            lastResponse = null;

            try {
                // 2. Send message to server
                sendToServer(msg);

                // 3. Wait for handleMessageFromServer to update state
                while (awaitResponse) {
                    try {
                        wait(); // Releases lock and waits for notify()
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Could not send message to server.");
                awaitResponse = false;
            }

            // 4. Return the received response
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
        System.exit(0);
    }
}