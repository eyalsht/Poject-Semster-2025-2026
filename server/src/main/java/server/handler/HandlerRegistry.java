package server.handler;

import common.messaging.Message;
import common.enums.ActionType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registry that maps action types to their handlers.
 * This is the ONLY place you need to register new handlers.
 */
public class HandlerRegistry {

    private static HandlerRegistry instance;
    
    private final Map<ActionType, RequestHandler> handlers = new EnumMap<>(ActionType.class);

    private HandlerRegistry() {}

    public static synchronized HandlerRegistry getInstance() {
        if (instance == null) {
            instance = new HandlerRegistry();
            instance.initialize();
        }
        return instance;
    }

    /**
     * Register all handlers.
     * To add a new feature: just add one line here!
     */
    private void initialize() {
        // Authentication
        register(ActionType.LOGIN_REQUEST, new LoginHandler());
        register(ActionType.REGISTER_REQUEST, new RegisterHandler());

        // Catalog
        register(ActionType.GET_CATALOG_REQUEST, new GetCatalogHandler());
        register(ActionType.GET_CITY_DETAILS_REQUEST, new GetCityDetailsHandler());

        // Price Management
        register(ActionType.UPDATE_PRICE_REQUEST, new UpdatePriceHandler());
        register(ActionType.GET_PENDING_APPROVALS_REQUEST, new GetPendingApprovalsHandler());
        register(ActionType.APPROVE_PENDING_REQUEST, new ApprovePendingHandler());
        register(ActionType.DENY_PENDING_REQUEST, new DenyPendingHandler());

        // Content Management (NEW)
        register(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, new SubmitContentChangeHandler());
        register(ActionType.GET_PENDING_CONTENT_APPROVALS_REQUEST, new GetPendingContentApprovalsHandler());
        // register(ActionType.APPROVE_CONTENT_REQUEST, new ApproveContentHandler());  // TODO
        // register(ActionType.DENY_CONTENT_REQUEST, new DenyContentHandler());        // TODO

        // Purchases
        register(ActionType.PURCHASE_REQUEST, new PurchaseHandler());
    }

    public void register(ActionType action, RequestHandler handler) {
        handlers.put(action, handler);
        System.out.println("Registered handler for: " + action);
    }

    public RequestHandler getHandler(ActionType action) {
        return handlers.get(action);
    }

    /**
     * Handle a request by finding and invoking the appropriate handler.
     */
    public Message handleRequest(Message request) {
        RequestHandler handler = handlers.get(request.getAction());
        
        if (handler == null) {
            System.err.println("No handler registered for action: " + request.getAction());
            return new Message(ActionType.ERROR, "Unknown action: " + request.getAction());
        }

        try {
            return handler.handle(request);
        } catch (Exception e) {
            System.err.println("Error in handler for " + request.getAction() + ": " + e.getMessage());
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Server error: " + e.getMessage());
        }
    }
}
