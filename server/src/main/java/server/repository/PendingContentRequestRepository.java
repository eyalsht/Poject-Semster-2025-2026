package server.repository;

import common.content.City;
import common.content.GCMMap;
import common.content.Site;
import common.enums.ContentActionType;
import common.enums.ContentType;
import common.enums.MapStatus;
import common.enums.RequestStatus;
import common.user.User;
import common.workflow.PendingContentRequest;
import org.hibernate.Session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PendingContentRequest entity operations.
 */
public class PendingContentRequestRepository extends BaseRepository<PendingContentRequest, Integer> {

    private static PendingContentRequestRepository instance;

    private PendingContentRequestRepository() {
        super(PendingContentRequest.class);
    }

    public static synchronized PendingContentRequestRepository getInstance() {
        if (instance == null) {
            instance = new PendingContentRequestRepository();
        }
        return instance;
    }

    /**
     * Create a new pending content request.
     */
    public boolean createPendingRequest(User requester, ContentActionType actionType,
                                         ContentType contentType, Integer targetId,
                                         String targetName, String contentDetails) {
        try {
            PendingContentRequest request = new PendingContentRequest(
                requester, actionType, contentType, targetId, targetName, contentDetails
            );
            save(request);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all pending content requests.
     */
    public List<PendingContentRequest> findAllPending() {
        return executeQuery(session ->
            session.createQuery(
                "FROM PendingContentRequest p " +
                "LEFT JOIN FETCH p.requester " +
                "WHERE p.status = :status " +
                "ORDER BY p.createdAt DESC",
                PendingContentRequest.class)
                .setParameter("status", RequestStatus.OPEN)
                .getResultList()
        );
    }

    /**
     * Count pending content requests.
     */
    public int countPending() {
        return executeQuery(session -> {
            Long count = session.createQuery(
                "SELECT COUNT(p) FROM PendingContentRequest p WHERE p.status = :status",
                Long.class)
                .setParameter("status", RequestStatus.OPEN)
                .uniqueResult();
            return count != null ? count.intValue() : 0;
        });
    }

    /**
     * Approve a pending content request.
     */
    public boolean approve(int pendingId, User approver) {
        try {
            executeInTransaction(session -> {
                PendingContentRequest pending = session.get(PendingContentRequest.class, pendingId);
                
                if (pending == null || pending.getStatus() != RequestStatus.OPEN) {
                    throw new RuntimeException("Pending request not found or already processed");
                }

                // Apply the actual change based on action type
                applyContentChange(session, pending);

                // Mark as approved
                pending.setStatus(RequestStatus.APPROVED);
                pending.setProcessedAt(LocalDateTime.now());
                pending.setProcessedBy(approver);
                session.merge(pending);
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deny a pending content request.
     */
    public boolean deny(int pendingId, User denier) {
        try {
            executeInTransaction(session -> {
                PendingContentRequest pending = session.get(PendingContentRequest.class, pendingId);
                
                if (pending == null || pending.getStatus() != RequestStatus.OPEN) {
                    throw new RuntimeException("Pending request not found or already processed");
                }

                pending.setStatus(RequestStatus.DENIED);
                pending.setProcessedAt(LocalDateTime.now());
                pending.setProcessedBy(denier);
                session.merge(pending);
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Apply the actual content change when approved.
     */
    private void applyContentChange(org.hibernate.Session session, PendingContentRequest pending) {
        ContentType contentType = pending.getContentType();
        
        switch (contentType) {
            case MAP:
                applyMapChange(session, pending);
                break;
            case SITE:
                applySiteChange(session, pending);
                break;
            case TOUR:
                applyTourChange(session, pending);
                break;
            case CITY:
                applyCityChanges(session,pending);
                break;
        }
    }

    private void applyCityChanges(org.hibernate.Session session, PendingContentRequest pending)
    {

    }

    private void applyMapChange(org.hibernate.Session session, PendingContentRequest pending) {
        String json = pending.getContentDetails();
        
        switch (pending.getActionType()) {
            case ADD:
                createNewMap(session, json);
                break;
            case EDIT:
                updateExistingMap(session, pending.getTargetId(), json);
                break;
            case DELETE:
                deleteMap(session, pending.getTargetId());
                break;
        }
    }

    /**
     * Create a new map from JSON content details.
     */
    private void createNewMap(org.hibernate.Session session, String json) {
        String cityName = extractJsonValue(json, "cityName");
        String mapName = extractJsonValue(json, "mapName");
        String description = extractJsonValue(json, "description");
        
        // Find or create the city
        City city = session.createQuery("FROM City c WHERE c.name = :name", City.class)
            .setParameter("name", cityName)
            .uniqueResultOptional()
            .orElse(null);
        
        if (city == null) {
            // Create new city if it doesn't exist
            city = new City();
            city.setName(cityName);
            city.setDescription("Auto-created for map: " + mapName);
            session.persist(city);
        }
        
        // Create the new map
        GCMMap newMap = new GCMMap();
        newMap.setName(mapName);
        newMap.setDescription(description);
        newMap.setVersion("1.0");
        newMap.setStatus(MapStatus.PUBLISHED);
        newMap.setPrice(0.0);  // Default price, to be set via price update workflow
        newMap.setCity(city);
        
        session.persist(newMap);
        System.out.println("Created new map: " + mapName + " in city: " + cityName);
    }

    /**
     * Update an existing map with new values from JSON.
     */
    private void updateExistingMap(org.hibernate.Session session, Integer mapId, String json) {
        if (mapId == null) {
            throw new RuntimeException("Cannot update map: targetId is null");
        }
        
        GCMMap map = session.get(GCMMap.class, mapId);
        if (map == null) {
            throw new RuntimeException("Map not found with ID: " + mapId);
        }
        
        // Extract values from JSON
        String description = extractJsonValue(json, "description");
        String mapName = extractJsonValue(json, "mapName");
        
        // Update only the fields that are allowed to be edited
        if (description != null && !description.isEmpty()) {
            map.setDescription(description);
            System.out.println("Updated map description for: " + map.getName());
        }
        
        // Map name update (if provided and different)
        if (mapName != null && !mapName.isEmpty() && !mapName.equals(map.getName())) {
            map.setName(mapName);
            System.out.println("Updated map name to: " + mapName);
        }
        
        session.merge(map);
        System.out.println("Map update applied for ID: " + mapId);
    }
    /**
     * Delete a map by ID.
     */
    private void deleteMap(org.hibernate.Session session, Integer mapId) {
        if (mapId == null) {
            throw new RuntimeException("Cannot delete map: targetId is null");
        }
        
        GCMMap map = session.get(GCMMap.class, mapId);
        if (map != null) {
            String mapName = map.getName();
            session.remove(map);
            System.out.println("Deleted map: " + mapName + " (ID: " + mapId + ")");
        } else {
            System.out.println("Map not found for deletion, ID: " + mapId);
        }
    }

    /**
     * Simple JSON value extractor.
     * Extracts the value for a given key from a JSON string.
     * Note: This is a basic implementation. Consider using a proper JSON library (Gson, Jackson) for production.
     */
    private String extractJsonValue(String json, String key) {
        if (json == null || key == null) return null;
        
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int valueStart = keyIndex + searchKey.length();
        
        // Skip whitespace
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart >= json.length()) return null;
        
        char firstChar = json.charAt(valueStart);
        
        if (firstChar == '"') {
            // String value
            int valueEnd = valueStart + 1;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (c == '"' && json.charAt(valueEnd - 1) != '\\') {
                    break;
                }
                valueEnd++;
            }
            String value = json.substring(valueStart + 1, valueEnd);
            // Unescape basic sequences
            return value.replace("\\\"", "\"")
                       .replace("\\n", "\n")
                       .replace("\\r", "\r")
                       .replace("\\\\", "\\");
        } else if (firstChar == '[') {
            // Array value - find matching bracket
            int bracketCount = 1;
            int valueEnd = valueStart + 1;
            while (valueEnd < json.length() && bracketCount > 0) {
                if (json.charAt(valueEnd) == '[') bracketCount++;
                if (json.charAt(valueEnd) == ']') bracketCount--;
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd);
        } else {
            // Number or other value
            int valueEnd = valueStart;
            while (valueEnd < json.length() && 
                   json.charAt(valueEnd) != ',' && 
                   json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    private void applySiteChange(org.hibernate.Session session, PendingContentRequest pending) {
        String json = pending.getContentDetails();
        Site site = new Site(extractJsonValue(json,"id"), extractJsonValue(json,"name"),extractJsonValue(json,"cityID"),)
    }

    private void applyTourChange(org.hibernate.Session session, PendingContentRequest pending) {
        // TODO: Implement tour changes
        System.out.println("Tour changes not yet implemented");
    }
}
