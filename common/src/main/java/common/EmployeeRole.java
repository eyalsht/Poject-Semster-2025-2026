package main.java.common;
/**
 * Represents the different job roles an employee can have in the GCM system.
 * This helps us know what permissions each user has.
 */
public enum EmployeeRole {
    Company_Manager,  // Can manage the whole system and view all reports
    Content_Manager,  // Can approve or reject maps and tours
    Content_Worker,   // Can create and edit content (maps, sites)
    Support_Agent     // Deals with user requests and support tickets
}