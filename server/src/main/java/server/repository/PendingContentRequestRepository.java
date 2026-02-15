package server.repository;

import common.content.City;
import common.content.GCMMap;
import common.content.Site;
import common.content.Tour;
import common.enums.*;
import common.user.User;
import common.workflow.PendingContentRequest;
import org.hibernate.Session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<PendingContentRequest> findAllPending()
    {
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
    public boolean approve(int pendingId, User approver)
    {
        return approveWithError(pendingId, approver) == null;
    }

    /**
     * Approve a pending request. Returns null on success, or an error message string on failure.
     */
    public String approveWithError(int pendingId, User approver)
    {
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
                //TODO implent the approver pending.setProcessedBy(approver);
                session.merge(pending);
            });
            return null; // success
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * Deny a pending content request.
     */
    public boolean deny(int pendingId, User denier)
    {
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
    private void applyContentChange(org.hibernate.Session session, PendingContentRequest pending)
    {
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
                applyCityChanges(session, pending);
                break;
        }
    }

    private void applyMapChange(org.hibernate.Session session, PendingContentRequest pending)
    {
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

    private void createNewMap(org.hibernate.Session session, String json)
    {
        // Check if this is an import from external repository
        String externalMapIdStr = extractJsonValue(json, "externalMapId");
        if (externalMapIdStr != null && !externalMapIdStr.isEmpty()) {
            importExternalMap(session, Integer.parseInt(externalMapIdStr));
            return;
        }

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
     * Import a map from the external repository into GCM.
     * Changes status from EXTERNAL to PUBLISHED (price remains 0, set via Price Update).
     */
    private void importExternalMap(org.hibernate.Session session, int externalMapId) {
        GCMMap map = session.get(GCMMap.class, externalMapId);
        if (map == null) {
            throw new RuntimeException("External map not found with ID: " + externalMapId);
        }
        if (map.getStatus() != MapStatus.EXTERNAL) {
            throw new RuntimeException("Map ID " + externalMapId + " is not in EXTERNAL status");
        }

        map.setStatus(MapStatus.PUBLISHED);
        session.merge(map);
        System.out.println("Imported external map: " + map.getName() + " (ID: " + externalMapId + ") -> PUBLISHED");
    }

    private void updateExistingMap(org.hibernate.Session session, Integer mapId, String json)
    {
        if (mapId == null) {
            throw new RuntimeException("Cannot update map: targetId is null");
        }

        GCMMap map = session.get(GCMMap.class, mapId);
        if (map == null) {
            throw new RuntimeException("Map not found with ID: " + mapId);
        }

        boolean contentChanged = false;

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

        // Map image update (Base64 encoded)
        String mapImageBase64 = extractJsonValue(json, "mapImage");
        if (mapImageBase64 != null && !mapImageBase64.isEmpty()) {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(mapImageBase64);
            map.setMapImage(imageBytes);
            contentChanged = true;
            System.out.println("Updated map image for: " + map.getName() + " (" + imageBytes.length + " bytes)");
        }

        // Site markers JSON update
        String siteMarkersJson = extractJsonValue(json, "siteMarkersJson");
        if (siteMarkersJson != null && !siteMarkersJson.isEmpty()) {
            map.setSiteMarkersJson(siteMarkersJson);
            contentChanged = true;
            System.out.println("Updated site markers for: " + map.getName());
        }

        // Site IDs update (comma-separated)
        String siteIdsRaw = extractJsonValue(json, "siteIds");
        if (siteIdsRaw != null && !siteIdsRaw.trim().isEmpty()) {
            map.getSites().clear();
            List<Integer> siteIds = Arrays.stream(siteIdsRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            for (Integer siteId : siteIds) {
                Site site = session.get(Site.class, siteId);
                if (site != null) {
                    map.addSite(site);
                }
            }
            contentChanged = true;
            System.out.println("Updated map sites for: " + map.getName() + " (" + siteIds.size() + " sites)");
        }

        // Bump version if content actually changed (sites, markers, image)
        if (contentChanged) {
            bumpMapVersion(map);
            System.out.println("Bumped map version to: " + map.getVersion());
        }

        session.merge(map);
        System.out.println("Map update applied for ID: " + mapId);
    }

    /**
     * Revert a map back to the external repository instead of hard-deleting.
     * Clears GCM-specific data (sites, markers, price) but keeps raw data (name, description, image, city).
     */
    private void deleteMap(org.hibernate.Session session, Integer mapId)
    {
        if (mapId == null) {
            throw new RuntimeException("Cannot delete map: targetId is null");
        }

        GCMMap map = session.get(GCMMap.class, mapId);
        if (map != null) {
            String mapName = map.getName();
            // Clear GCM-specific data
            map.getSites().clear();
            map.setSiteMarkersJson(null);
            map.setPrice(0);
            map.setVersion("1.0");
            map.setStatus(MapStatus.EXTERNAL);
            // Keep: name, description, mapImage, city (the "raw" external data)
            session.merge(map);
            System.out.println("Reverted map to external: " + mapName + " (ID: " + mapId + ")");
        } else {
            System.out.println("Map not found for revert, ID: " + mapId);
        }
    }

    private void applySiteChange(org.hibernate.Session session, PendingContentRequest pending)
    {
        switch (pending.getActionType()) {
            case ADD:
                createNewSite(session, pending);
                break;
            case EDIT:
                updateExistingSite(session, pending.getTargetId(), pending);
                break;
            case DELETE:
                deleteSite(session, pending.getTargetId(), pending);
                break;
        }
    }

    private void createNewSite(org.hibernate.Session session, PendingContentRequest pending)
    {
        String json = pending.getContentDetails();
        CityRepository cp = CityRepository.getInstance();
        int cityId = pending.getTargetId();
        City city = session.get(City.class, cityId);
        if (city == null) {
            System.err.println("CRITICAL: City ID " + cityId + " not found in DB!");
            return;
        }

        Site site = new Site();
        site.setName(extractJsonValue(json, "name"));
        site.setDescription(extractJsonValue(json, "description"));
        site.setLocation(extractJsonValue(json, "location"));
        site.setCity(city);
        site.setCategory(SiteCategory.valueOf(extractJsonValue(json, "category")));
        site.setRecommendedVisitDuration(SiteDuration.fromLabel(extractJsonValue(json, "recommendedVisitDuration")));
        site.setAccessible(Boolean.parseBoolean(extractJsonValue(json, "isAccessible")));

        city.addSite(site);
        session.persist(site);
        System.out.println("Created new site: " + extractJsonValue(json,"name") + " in city: " + city.getName());
    }

    private void updateExistingSite(org.hibernate.Session session, int siteID, PendingContentRequest pending)
    {
        String json = pending.getContentDetails();
        Site existingSite = session.get(Site.class, siteID);

        if (existingSite != null)
        {
            existingSite.setName(extractJsonValue(json, "name"));
            existingSite.setDescription(extractJsonValue(json, "description"));
            existingSite.setLocation(extractJsonValue(json, "location"));
            existingSite.setCategory(SiteCategory.valueOf(extractJsonValue(json, "category")));
            existingSite.setAccessible(Boolean.parseBoolean(extractJsonValue(json, "isAccessible")));
            existingSite.setRecommendedVisitDuration(
                    SiteDuration.fromLabel(extractJsonValue(json, "recommendedVisitDuration"))
            );
            System.out.println("Updated existing site: " + existingSite.getName() + " (ID: " + siteID + ")");
        } else {
            System.err.println("Error: Could not find site with ID " + siteID + " to update.");
        }
    }

    private void deleteSite(org.hibernate.Session session, int siteID, PendingContentRequest pending)
    {
        Site siteToDelete = session.get(Site.class, siteID);
        if (siteToDelete != null)
        {
            City city = siteToDelete.getCity();
            if (city != null) {
                city.getSites().remove(siteToDelete);
            }
            for (Tour tour : siteToDelete.getTours()) {
                tour.getSites().remove(siteToDelete);
            }

            for (GCMMap map : siteToDelete.getMaps()) {
                map.getSites().remove(siteToDelete);
            }
            session.remove(siteToDelete);
            System.out.println("Successfully deleted site: " + siteToDelete.getName() + " (ID: " + siteID + ")");
        } else {
            System.err.println("Error: Could not find site with ID " + siteID + " to delete.");
        }
    }

    private void applyTourChange(org.hibernate.Session session, PendingContentRequest pending)
    {
        switch (pending.getActionType()) {
            case ADD:
                createNewTour(session, pending);
                break;
            case EDIT:
                updateExistingTour(session, pending.getTargetId(), pending);
                break;
            case DELETE:
                deleteTour(session, pending.getTargetId(), pending);
                break;
        }
    }

    private void createNewTour(Session session, PendingContentRequest pending)
    {
        String json = pending.getContentDetails();
        CityRepository cp = CityRepository.getInstance();
        int cityId = pending.getTargetId();
        City city = session.get(City.class, cityId);
        if (city == null) {
            System.err.println("CRITICAL: City ID " + cityId + " not found in DB!");
            return;
        }

        Tour tour = new Tour();
        tour.setName(extractJsonValue(json, "name"));
        tour.setDescription(extractJsonValue(json, "description"));
        tour.setCity(city);
        String siteIdsRaw = extractJsonValue(json, "siteIds");
        List<Integer> siteIds = new ArrayList<>();
        if (siteIdsRaw != null && !siteIdsRaw.trim().isEmpty())
        {
            siteIds = Arrays.stream(siteIdsRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
        for (Integer siteId : siteIds)
        {
            Site site = session.get(Site.class, siteId);
            if (site != null) {
                tour.addSite(site);
            }
        }

        // Use manually-set duration if provided, otherwise auto-calculate from sites
        String duration = extractJsonValue(json, "recommendedDuration");
        if (duration != null && !duration.trim().isEmpty()) {
            tour.setRecommendedDuration(duration);
        } else {
            updateTourDuration(tour);
        }

        city.addTour(tour);

        session.persist(tour);
        System.out.println("Created new site: " + extractJsonValue(json,"name") + " in city: " + city.getName());
    }

    private void updateExistingTour(Session session, Integer targetId, PendingContentRequest pending) {
        if (targetId == null)
        {
            System.err.println("Error: Cannot update tour without targetId.");
            return;
        }
        String json = pending.getContentDetails();
        Tour existingTour = session.get(Tour.class, targetId);

        if (existingTour != null)
        {
            existingTour.setName(extractJsonValue(json, "name"));
            existingTour.setDescription(extractJsonValue(json, "description"));

            String siteIdsRaw = extractJsonValue(json, "siteIds");
            List<Integer> newSiteIds = new ArrayList<>();
            if (siteIdsRaw != null && !siteIdsRaw.trim().isEmpty())
            {
                newSiteIds = Arrays.stream(siteIdsRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            }

            existingTour.getSites().clear();
            for (Integer sId : newSiteIds)
            {
                Site site = session.get(Site.class, sId);
                if (site != null) {
                    existingTour.addSite(site);
                }
            }

            // Use manually-set duration if provided, otherwise auto-calculate from sites
            String duration = extractJsonValue(json, "recommendedDuration");
            if (duration != null && !duration.trim().isEmpty()) {
                existingTour.setRecommendedDuration(duration);
            } else {
                updateTourDuration(existingTour);
            }
            session.merge(existingTour);
            session.merge(existingTour);
            System.out.println("Approved Update: Tour " + existingTour.getName() + " (ID: " + targetId + ")");
        }
        else
        {
            System.err.println("Error: Could not find Tour with ID " + targetId + " to update.");
        }
    }

    private void deleteTour(Session session, Integer targetId, PendingContentRequest pending) {
        if (targetId == null)
            return;
        Tour tourToDelete = session.get(Tour.class, targetId);
        if (tourToDelete != null)
        {
            City city = tourToDelete.getCity();
            if (city != null)
            {
                city.getTours().remove(tourToDelete);
            }
            tourToDelete.getSites().clear();
            session.remove(tourToDelete);
            System.out.println("Approved Delete: Tour ID " + targetId);
        } else {
            System.err.println("Error: Could not find Tour with ID " + targetId + " to delete.");
        }
    }

    private void applyCityChanges(org.hibernate.Session session, PendingContentRequest pending) {
        if (pending.getActionType() != ContentActionType.EDIT) {
            System.err.println("City changes only support EDIT action type.");
            return;
        }

        String json = pending.getContentDetails();
        String cityIdStr = extractJsonValue(json, "cityId");
        if (cityIdStr == null) {
            System.err.println("No cityId found in city change request JSON.");
            return;
        }

        int cityId = Integer.parseInt(cityIdStr);
        City city = session.get(City.class, cityId);
        if (city == null) {
            System.err.println("City not found with ID: " + cityId);
            return;
        }

        String name = extractJsonValue(json, "name");
        if (name != null && !name.isEmpty()) {
            city.setName(name);
            System.out.println("Updated city name to: " + name);
        }

        String description = extractJsonValue(json, "description");
        if (description != null && !description.isEmpty()) {
            city.setDescription(description);
            System.out.println("Updated city description for: " + city.getName());
        }

        session.merge(city);
        System.out.println("City update applied for ID: " + cityId);
    }

    private void bumpMapVersion(GCMMap map) {
        String ver = map.getVersion();
        if (ver == null || ver.isEmpty()) {
            map.setVersion("1.1");
            return;
        }
        try {
            double v = Double.parseDouble(ver);
            map.setVersion(String.format("%.1f", v + 0.1));
        } catch (NumberFormatException e) {
            map.setVersion(ver + ".1");
        }
    }

    private void updateTourDuration(Tour tour)
    {
        double totalHours = 0.0;
        for (Site site : tour.getSites()) {
            if (site.getRecommendedVisitDuration() != null) {
                totalHours += site.getRecommendedVisitDuration().getHours();
            }
        }
        String finalDuration;
        if (totalHours == 0) {
            finalDuration = "0 hours";
        } else {
            // Use "hour" for exactly 1, "hours" for everything else
            String unit = (totalHours == 1.0) ? " hour" : " hours";

            // Remove trailing .0 for whole numbers (e.g., "3 hours" instead of "3.0 hours")
            if (totalHours == (long) totalHours) {
                finalDuration = String.format("%d%s", (long)totalHours, unit);
            } else {
                finalDuration = String.format("%.1f%s", totalHours, unit);
            }
        }
        tour.setRecommendedDuration(finalDuration);
    }
    private String extractJsonValue(String json, String key)
    {
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
}
