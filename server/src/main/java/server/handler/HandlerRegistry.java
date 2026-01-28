package server.handler;

import common.messaging.Message;
import common.enums.ActionType;
import server.report.ReportManager;

import java.util.EnumMap;
import java.util.Map;
import server.HibernateUtil;

/**
 * Registry that maps action types to their handlers.
 * This is the ONLY place you need to register new handlers.
 */
public class HandlerRegistry {

    private static HandlerRegistry instance;
    
    private final Map<ActionType, RequestHandler> handlers = new EnumMap<>(ActionType.class);

    private ReportManager reportManager;

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
    private void initialize()
    {
        // Authentication
        register(ActionType.LOGIN_REQUEST, new LoginHandler());
        register(ActionType.REGISTER_REQUEST, new RegisterHandler());

        // Catalog
        register(ActionType.GET_CATALOG_REQUEST, new GetCatalogHandler());
        register(ActionType.GET_CITY_DETAILS_REQUEST, new GetCityDetailsHandler());
        register(ActionType.GET_CITIES_REQUEST,new GetCitiesHandler());
        register(ActionType.GET_CITY_SITES_REQUEST, new GetCitySitesHandler());
        register(ActionType.GET_CITY_TOURS_REQUEST, new GetCityToursHandler());

        // Price Management
        register(ActionType.UPDATE_PRICE_REQUEST, new UpdatePriceHandler());
        register(ActionType.GET_PENDING_APPROVALS_REQUEST, new GetPendingApprovalsHandler());
        register(ActionType.APPROVE_PENDING_REQUEST, new ApprovePendingHandler());
        register(ActionType.DENY_PENDING_REQUEST, new DenyPendingHandler());

        // Content Management
        register(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, new SubmitContentChangeHandler());
        register(ActionType.GET_PENDING_CONTENT_APPROVALS_REQUEST, new GetPendingContentApprovalsHandler());
        register(ActionType.APPROVE_CONTENT_REQUEST, new ApproveContentHandler());
        register(ActionType.DENY_CONTENT_REQUEST, new DenyContentHandler());

        // Purchases
        register(ActionType.PURCHASE_REQUEST, new PurchaseHandler());

        // Reports
        reportManager = new ReportManager(HibernateUtil.getSessionFactory());

        register(ActionType.GET_ALL_CLIENTS_REPORT_REQUEST,
                new GetAllClientsReportHandler(reportManager));

        register(ActionType.GET_ACTIVITY_REPORT_REQUEST,
                new GetActivityReportHandler(reportManager));

        register(ActionType.LOG_MAP_VIEW_REQUEST,
                new LogMapViewHandler(HibernateUtil.getSessionFactory()));

        register(ActionType.LOG_MAP_DOWNLOAD_REQUEST,
                new LogMapDownloadHandler(HibernateUtil.getSessionFactory()));
        register(ActionType.GET_PURCHASES_REPORT_REQUEST,
                new GetPurchasesReportHandler(reportManager));

        //Support
        register(ActionType.SUBMIT_SUPPORT_REQUEST, new SubmitSupportHandler());
        register(ActionType.CREATE_SUPPORT_TICKET, new CreateSupportTicketHandler());
        register(ActionType.LIST_SUPPORT_TICKETS, new ListSupportTicketsHandler());
        register(ActionType.REPLY_SUPPORT_TICKET, new ReplySupportTicketHandler());
        register(ActionType.LIST_CLIENT_SUPPORT_REPLIES, new ListClientSupportRepliesHandler());
        register(ActionType.MARK_SUPPORT_REPLY_READ, new MarkSupportReplyReadHandler());

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
    public ReportManager getReportManager() {
        return reportManager;
    }

}
