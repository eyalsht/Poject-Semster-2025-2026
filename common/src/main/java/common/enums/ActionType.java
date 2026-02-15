package common.enums;

/**
 * Defines all message action types for client-server communication.
 * 
 * Naming Convention:
 * - Requests end with _REQUEST
 * - Responses end with _RESPONSE
 * - Each request should have a corresponding response
 */
public enum ActionType {
    
    // ==================== AUTHENTICATION ====================
    LOGIN_REQUEST,
    LOGIN_RESPONSE,          // Replaces LOGIN_SUCCESS/LOGIN_FAILED (success determined by payload)
    
    REGISTER_REQUEST,
    REGISTER_RESPONSE,       // Replaces REGISTER_SUCCESS/REGISTER_FAILED

    LOGOUT_REQUEST,
    LOGOUT_RESPONSE,

    // ==================== CATALOG ====================
    // Single request for all catalog data (cities, maps, filters)
    GET_CATALOG_REQUEST,
    GET_CATALOG_RESPONSE,

    // Get all cities
    GET_CITIES_REQUEST,
    GET_CITIES_RESPONSE,

    // Get all cities sites
    GET_CITY_SITES_REQUEST,
    GET_CITY_SITES_RESPONSE,

    // Get all city tours
    GET_CITY_TOURS_REQUEST,
    GET_CITY_TOURS_RESPONSE,

    // Get full details for a specific city (with all maps, sites, tours)
    GET_CITY_DETAILS_REQUEST,
    GET_CITY_DETAILS_RESPONSE,

    // Get full city data including map images, markers, sites, tours (for Edit Mode)
    GET_CITY_FULL_DETAILS_REQUEST,
    GET_CITY_FULL_DETAILS_RESPONSE,

    // Get a single map with full data (image, sites, markers) for map popup
    GET_MAP_DETAILS_REQUEST,
    GET_MAP_DETAILS_RESPONSE,
    
    // ==================== PRICE MANAGEMENT ====================

    UPDATE_PRICE_REQUEST,
    UPDATE_PRICE_RESPONSE,
    
    GET_PENDING_PRICE_APPROVALS_REQUEST,    // Renamed for clarity
    GET_PENDING_PRICE_APPROVALS_RESPONSE,
    
    APPROVE_PRICE_REQUEST,                   // Renamed for clarity
    APPROVE_PRICE_RESPONSE,
    
    DENY_PRICE_REQUEST,                      // Renamed for clarity
    DENY_PRICE_RESPONSE,

    // ==================== CONTENT MANAGEMENT ====================
    // Content change requests (add/edit/delete maps, sites, tours)
    SUBMIT_CONTENT_CHANGE_REQUEST,
    SUBMIT_CONTENT_CHANGE_RESPONSE,
    
    GET_PENDING_CONTENT_APPROVALS_REQUEST,
    GET_PENDING_CONTENT_APPROVALS_RESPONSE,
    
    APPROVE_CONTENT_REQUEST,
    APPROVE_CONTENT_RESPONSE,
    
    DENY_CONTENT_REQUEST,
    DENY_CONTENT_RESPONSE,

    // Legacy support (can be deprecated later)
    GET_PENDING_APPROVALS_REQUEST,
    GET_PENDING_APPROVALS_RESPONSE,
    
    APPROVE_PENDING_REQUEST,
    APPROVE_PENDING_RESPONSE,
    
    DENY_PENDING_REQUEST,
    DENY_PENDING_RESPONSE,
    
    // ==================== PURCHASES ====================
    PURCHASE_REQUEST,
    PURCHASE_RESPONSE,

    //For reports added by renat
    GET_ALL_CLIENTS_REPORT,
    GET_ALL_CLIENTS_REPORT_REQUEST,
    GET_ALL_CLIENTS_REPORT_RESPONSE,

    // Activity report + stats logging
    LOG_MAP_VIEW_REQUEST,
    LOG_MAP_VIEW_RESPONSE,

    LOG_MAP_DOWNLOAD_REQUEST,
    LOG_MAP_DOWNLOAD_RESPONSE,

    GET_ACTIVITY_REPORT_REQUEST,
    GET_ACTIVITY_REPORT_RESPONSE,

    GET_USER_SUBSCRIPTIONS_REQUEST,
    GET_USER_SUBSCRIPTIONS_RESPONSE,

    GET_USER_PURCHASED_MAPS_REQUEST,
    GET_USER_PURCHASED_MAPS_RESPONSE,

    CHECK_SUBSCRIPTION_STATUS_REQUEST,
    CHECK_SUBSCRIPTION_STATUS_RESPONSE,

    CHECK_MAP_PURCHASE_STATUS_REQUEST,
    CHECK_MAP_PURCHASE_STATUS_RESPONSE,

    GET_SUBSCRIPTION_MAPS_REQUEST,
    GET_SUBSCRIPTION_MAPS_RESPONSE,

    UPDATE_PAYMENT_DETAILS_REQUEST,
    UPDATE_PAYMENT_DETAILS_RESPONSE,

    //Purchase report +other reports
    GET_PURCHASES_REPORT_REQUEST,
    GET_PURCHASES_REPORT_RESPONSE,
    //Support request report
    GET_SUPPORT_REQUESTS_REPORT_REQUEST,
    GET_SUPPORT_REQUESTS_REPORT_RESPONSE,

    //Bot+ Support
    SUBMIT_SUPPORT_REQUEST,
    SUBMIT_SUPPORT_RESPONSE,

    //Support agent
    CREATE_SUPPORT_TICKET,
    LIST_SUPPORT_TICKETS,
    REPLY_SUPPORT_TICKET,
    LIST_CLIENT_SUPPORT_REPLIES,
    MARK_SUPPORT_REPLY_READ,
    // ==================== EXTERNAL MAP SYSTEM ====================
    GET_EXTERNAL_MAPS_REQUEST,
    GET_EXTERNAL_MAPS_RESPONSE,

    ADD_EXTERNAL_MAP_REQUEST,       // Temp: add map to external repository
    ADD_EXTERNAL_MAP_RESPONSE,

    ADD_CITY_REQUEST,               // Temp: create a new city
    ADD_CITY_RESPONSE,

    // ==================== NOTIFICATIONS ====================
    CATALOG_UPDATED_NOTIFICATION,

    // ==================== GENERIC ====================
    ERROR
}