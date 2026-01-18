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
    
    // ==================== CATALOG ====================
    // Single request for all catalog data (cities, maps, filters)
    GET_CATALOG_REQUEST,
    GET_CATALOG_RESPONSE,

    // Get all cities
    GET_CITIES_REQUEST,
    GET_CITIES_RESPONSE,

    // Get full details for a specific city (with all maps, sites, tours)
    GET_CITY_DETAILS_REQUEST,
    GET_CITY_DETAILS_RESPONSE,
    
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
    
    // ==================== GENERIC ====================
    ERROR;

}