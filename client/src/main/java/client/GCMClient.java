package client;

import common.enums.ActionType;
import common.messaging.Message;
import common.user.User;
import oscf.AbstractClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Singleton client for communication with the GCM server.
 * Uses synchronous request-response pattern via sendRequest().
 */
public class GCMClient extends AbstractClient {

    private static GCMClient instance;

    // Synchronization state for request-response
    private boolean busy = false;
    private boolean awaitResponse = false;
    private Object lastResponse = null;

    // Current logged-in user
    private volatile User currentUser;

    // Notification listeners for server-pushed messages
    private final List<Consumer<Message>> notificationListeners = new CopyOnWriteArrayList<>();

    // ==================== SINGLETON PATTERN ====================

    /**
     * Private constructor - use connect() to create instance.
     */
    private GCMClient(String host, int port) throws IOException {
        super(host, port);
        openConnection();
        System.out.println("GCMClient connected to server at " + host + ":" + port);
    }

    /**
     * Connect to the server. Must be called before getInstance().
     *
     * @param host Server hostname or IP
     * @param port Server port
     * @throws IOException if connection fails
     */
    public static void connect(String host, int port) throws IOException {
        if (instance == null) {
            instance = new GCMClient(host, port);
        }
    }

    /**
     * Get the singleton instance.
     *
     * @return The GCMClient instance
     * @throws IllegalStateException if connect() hasn't been called
     */
    public static GCMClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "GCMClient not connected. Call GCMClient.connect(host, port) first."
            );
        }
        return instance;
    }

    /**
     * Get existing instance without throwing exception.
     *
     * @return The instance or null if not connected
     */
    public static GCMClient getExistingInstance() {
        return instance;
    }

    /**
     * Check if the singleton client is initialized and connected to the server.
     *
     * @return true if instance exists and connection is open
     */
    public static boolean isClientConnected() {
        return instance != null && instance.isConnected();
    }

    // ==================== COMMUNICATION ====================

    /**
     * Handle message received from server.
     * Notifies waiting sendRequest() call.
     */
    @Override
    protected void handleMessageFromServer(Object msg) {
        System.out.println("GCMClient received: " + msg);

        // Check if this is a server-pushed notification or fire-and-forget response
        if (msg instanceof Message message) {
            ActionType action = message.getAction();
            if (action == ActionType.CATALOG_UPDATED_NOTIFICATION) {
                for (Consumer<Message> listener : notificationListeners) {
                    listener.accept(message);
                }
                return;
            }
            if (action == ActionType.LOGOUT_RESPONSE) {
                return; // fire-and-forget — don't interfere with request-response flow
            }
        }

        synchronized (this) {
            this.lastResponse = msg;
            this.awaitResponse = false;
            this.notifyAll();
        }
    }

    /**
     * Send a request to the server and wait for response.
     * This is a synchronous (blocking) call.
     *
     * @param request The request message to send
     * @return The response from server, or null if error
     */
    public Object sendRequest(Object request) {
        synchronized (this) {
            // Wait if another request is in progress
            while (busy) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }

            busy = true;
            awaitResponse = true;
            lastResponse = null;

            try {
                sendToServer(request);

                // Wait for response
                while (awaitResponse) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }

            } catch (IOException e) {
                System.err.println("Failed to send message to server: " + e.getMessage());
                e.printStackTrace();
                return null;
            } finally {
                busy = false;
                notifyAll();
            }

            return lastResponse;
        }
    }

    /**
     * Send a request and cast the response to Message.
     * Convenience method for type-safe communication.
     *
     * @param request The request message
     * @return The response as Message, or null if error
     */
    public Message sendMessage(Message request) {
        Object response = sendRequest(request);
        if (response instanceof Message) {
            return (Message) response;
        }
        return null;
    }

    // ==================== USER SESSION ====================

    /**
     * Get the currently logged-in user.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Set the current user (called after successful login).
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Clear the current user (called on logout).
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Check if a user is currently logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // ==================== NOTIFICATION LISTENERS ====================

    public void addNotificationListener(Consumer<Message> listener) {
        notificationListeners.add(listener);
    }

    public void removeNotificationListener(Consumer<Message> listener) {
        notificationListeners.remove(listener);
    }

    // ==================== LIFECYCLE ====================

    /**
     * Disconnect from server and cleanup.
     */
    public void quit() {
        try {
            if (currentUser != null) {
                try {
                    sendToServer(new Message(ActionType.LOGOUT_REQUEST, null));
                } catch (Exception e) {
                    // Server-side clientDisconnected/clientException will clean up
                }
            }
            logout();
            closeConnection();
            instance = null;
            System.out.println("GCMClient disconnected.");
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }

    /**
     * Called when connection to server is closed.
     */
    @Override
    protected void connectionClosed() {
        System.out.println("Connection to server closed.");
    }

    /**
     * Called when an exception occurs in connection.
     */
    @Override
    protected void connectionException(Exception exception) {
        System.err.println("Connection error: " + exception.getMessage());
    }
}