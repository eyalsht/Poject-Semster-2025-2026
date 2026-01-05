package client;

import java.io.IOException;

import common.Message;
import common.User;
import controllers.LoginPageController;
import javafx.application.Platform;
import oscf.AbstractClient;


public class GCMClient extends AbstractClient
{
    private static GCMClient instance;
    private LoginPageController loginController;
    private boolean busy = false;
    private boolean awaitResponse = false;
    private Object lastResponse = null;;

    /**
     * Private constructor implements the Singleton pattern.
     * Connects to the server immediately upon creation.
     */
    public void setLoginController(LoginPageController controller)
    {
        this.loginController = controller;
    }

    private GCMClient(String host, int port) throws IOException {
        super(host, port);
        System.out.println("Before openConnection()");
        openConnection();
        System.out.println("After openConnection()");
        System.out.println("GCMClient connected to server on " + host + ":" + port);
    }

    /**
     * Returns the single instance of the client.
     * If it doesn't exist, it creates it.
     */
   /* public static GCMClient getInstance()
    {
        if (instance == null)
        {
            try {
                //instance = new GCMClient("20.250.162.225", 5555);
                instance = new GCMClient("localhost", 5555);
            } catch (IOException e) {
                System.err.println("WARNING: Failed to connect to server. Make sure the server is running on 20.250.162.225:5555");
                e.printStackTrace();
            }
        }
        if(instance!=null) {
            return instance;
        }
        return null;
    } */
    public static GCMClient getInstance()
    {
        if (instance == null) {
            throw new IllegalStateException(
                    "GCMClient not connected yet. Call GCMClient.connect(host, port) first."
            );
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

        if (loginController != null && msg instanceof Message m) {
            Platform.runLater(() -> loginController.onServerMessage(m));
        }

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
        synchronized (this)
        {
            while (busy) {
                try { wait(); } catch (InterruptedException e) { e.printStackTrace(); }
            }
            busy = true;
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
                notifyAll();
            }
            return lastResponse;
        }
    }

    public void quit() {
        try {
            closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void connect(String host, int port) throws IOException {
        if (instance == null)
        {
            instance = new GCMClient(host, port);
        }
    }
    public static GCMClient getExistingInstance()
    {
        return instance;
    }

    private volatile User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

}