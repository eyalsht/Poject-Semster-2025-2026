package server;

import common.messaging.Message;
import common.enums.ActionType;
import server.handler.HandlerRegistry;
import server.ocsf.AbstractServer;
import server.ocsf.ConnectionToClient;

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

    private final HandlerRegistry handlerRegistry;

    public GcmServer(int port) {
        super(port);
        this.handlerRegistry = HandlerRegistry.getInstance();
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if (!(msg instanceof Message request)) {
            System.err.println("Warning: Received non-Message object from " + client);
            return;
        }

        System.out.println("Request: " + request.getAction() + " from " + client);

        try {
            // Delegate to handler registry
            Message response = handlerRegistry.handleRequest(request);

            if (response != null) {
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

        try {
            HibernateUtil.initialize("dbHibernate.cfg.xml");
            System.out.println("✓ Database connected via Hibernate");
            System.out.println("✓ Handler registry initialized");
            System.out.println("Server ready to accept connections.");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize server:");
            e.printStackTrace();
        }
    }

    @Override
    protected void serverStopped() {
        System.out.println("Server shutting down...");
        HibernateUtil.shutdown();
        System.out.println("Server stopped.");
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        System.out.println("Client connected: " + client);
    }

    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        System.out.println("Client disconnected: " + client);
    }
}
