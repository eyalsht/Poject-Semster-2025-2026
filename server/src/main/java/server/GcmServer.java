package server;

import common.dto.AuthResponse;
import common.messaging.Message;
import common.enums.ActionType;
import server.handler.HandlerRegistry;
import server.ocsf.AbstractServer;
import server.ocsf.ConnectionToClient;

import java.util.concurrent.ConcurrentHashMap;

/**
 * GCM Server - handles client connections and routes requests to handlers.
 * 
 * This class follows the Single Responsibility Principle:
 * - Manages connections
 * - Routes messages to appropriate handlers
 * - Handles server lifecycle
 * 
 * All business logic is delegated to Handler classes.
 */
public class GcmServer extends AbstractServer {

    private static GcmServer instance;

    private final HandlerRegistry handlerRegistry;
    private final ConcurrentHashMap<String, ConnectionToClient> activeSessions = new ConcurrentHashMap<>();

    public GcmServer(int port) {
        super(port);
        this.handlerRegistry = HandlerRegistry.getInstance();
        instance = this;
    }

    public static GcmServer getInstance() {
        return instance;
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if (!(msg instanceof Message request)) {
            System.err.println("Warning: Received non-Message object from " + client);
            return;
        }

        System.out.println("Request: " + request.getAction() + " from " + client);

        try {
            // Handle logout directly — it's a connection-layer concern, not business logic
            if (request.getAction() == ActionType.LOGOUT_REQUEST) {
                handleLogout(client);
                client.sendToClient(new Message(ActionType.LOGOUT_RESPONSE, "Logged out"));
                System.out.println("Response: LOGOUT_RESPONSE sent to " + client);
                return;
            }

            // Delegate to handler registry
            Message response = handlerRegistry.handleRequest(request);

            if (response != null) {
                // Intercept successful login responses to enforce unique sessions
                if (response.getAction() == ActionType.LOGIN_RESPONSE) {
                    response = enforceUniqueSession(response, client);
                }

                client.sendToClient(response);
                System.out.println("Response: " + response.getAction() + " sent to " + client);
            }

        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
            e.printStackTrace();

            try {
                client.sendToClient(new Message(ActionType.ERROR, "Server error: " + e.getMessage()));
            } catch (Exception sendError) {
                System.err.println("Failed to send error response: " + sendError.getMessage());
            }
        }
    }

    @Override
    protected void serverStarted() {
        System.out.println("===========================================");
        System.out.println("  GCM Server started on port " + getPort());
        System.out.println("===========================================");

        /*try {
            HibernateUtil.initialize("dbHibernate.cfg.xml");
            System.out.println("✓ Database connected via Hibernate");
            System.out.println("✓ Handler registry initialized");
            System.out.println("Server ready to accept connections.");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize server:");
            e.printStackTrace();
        }

         */
    }

    // ==================== SESSION MANAGEMENT ====================

    /**
     * Enforces that only one session per username is active at a time.
     * If the username is already logged in from another connection, the response
     * is replaced with a failure.
     */
    private Message enforceUniqueSession(Message response, ConnectionToClient client) {
        Object payload = response.getMessage();
        if (!(payload instanceof AuthResponse auth) || !auth.isSuccess()) {
            return response; // not a successful login — pass through
        }

        String username = auth.getUser().getEmail();
        ConnectionToClient existing = activeSessions.putIfAbsent(username, client);

        if (existing != null) {
            // Another session exists for this username
            if (existing.isAlive()) {
                // Existing session is still active — reject new login
                System.out.println("Duplicate login blocked for: " + username);
                return new Message(ActionType.LOGIN_RESPONSE,
                        AuthResponse.failure("This account is already logged in from another location."));
            }
            // Existing session is dead/stale — replace it
            activeSessions.replace(username, existing, client);
            // If replace failed (race), try putIfAbsent again
            if (activeSessions.putIfAbsent(username, client) != null
                    && activeSessions.get(username) != client) {
                return new Message(ActionType.LOGIN_RESPONSE,
                        AuthResponse.failure("This account is already logged in from another location."));
            }
        }

        client.setInfo("username", username);
        System.out.println("Session registered for: " + username);
        return response;
    }

    /**
     * Removes the session for the given client connection.
     * Uses two-arg remove to prevent one client's logout from removing another client's session.
     */
    private void handleLogout(ConnectionToClient client) {
        Object username = client.getInfo("username");
        if (username instanceof String name) {
            boolean removed = activeSessions.remove(name, client);
            client.setInfo("username", null);
            if (removed) {
                System.out.println("Session removed for: " + name);
            }
        }
    }

    // ==================== SERVER LIFECYCLE ====================

    @Override
    protected void serverStopped() {
        System.out.println("Server shutting down...");
        activeSessions.clear();
        HibernateUtil.shutdown();
        System.out.println("Server stopped.");
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        System.out.println("Client connected: " + client);
    }

    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {
        handleLogout(client);
        System.out.println("Client disconnected: " + client);
    }

    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable exception) {
        handleLogout(client);
        System.out.println("Client exception: " + client + " — " + exception.getMessage());
    }
}
