package server.handler;

import common.messaging.Message;

/**
 * Interface for handling client requests.
 * Each action type has its own handler implementation.
 */
@FunctionalInterface
public interface RequestHandler {
    
    /**
     * Handle a request and return a response.
     * 
     * @param request The incoming message from the client
     * @return The response message to send back
     */
    Message handle(Message request);
}
