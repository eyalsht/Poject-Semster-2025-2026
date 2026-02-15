package server.handler;

import common.content.City;
import common.enums.ActionType;
import common.enums.MapStatus;
import common.messaging.Message;
import org.hibernate.Session;
import org.hibernate.Transaction;
import server.HibernateUtil;
import server.repository.CityRepository;

import java.util.List;
import java.util.Optional;

/**
 * Temporary handler for adding maps to the simulated external repository.
 * Receives a List containing: [mapName, description, cityName, imageBytes].
 */
public class AddExternalMapHandler implements RequestHandler {

    private final CityRepository cityRepository = CityRepository.getInstance();

    @Override
    public Message handle(Message request) {
        Transaction tx = null;
        try {
            @SuppressWarnings("unchecked")
            List<Object> params = (List<Object>) request.getMessage();

            String mapName = (String) params.get(0);
            String description = (String) params.get(1);
            String cityName = (String) params.get(2);
            byte[] imageBytes = (byte[]) params.get(3);

            System.out.println("AddExternalMap: received request - map=" + mapName
                    + ", city=" + cityName
                    + ", imageSize=" + (imageBytes != null ? imageBytes.length : "null"));

            if (mapName == null || mapName.isBlank() || cityName == null || cityName.isBlank()) {
                System.err.println("AddExternalMap: mapName or cityName is blank");
                return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
            }

            if (imageBytes == null || imageBytes.length == 0) {
                System.err.println("AddExternalMap: imageBytes is null or empty");
                return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
            }

            Session session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // Find existing city ID or create new one
            // IMPORTANT: Do NOT load City entity via session.get() — City has 3 EAGER
            // collections (maps, sites, tours) which causes MultipleBagFetchException.
            int cityId;
            Optional<City> existingCity = cityRepository.findByName(cityName);
            if (existingCity.isPresent()) {
                cityId = existingCity.get().getId();
            } else {
                // Create city via native SQL to avoid eager-loading issues
                session.createNativeQuery(
                    "INSERT INTO cities (name, description, price_sub) VALUES (:name, :desc, :price)")
                    .setParameter("name", cityName)
                    .setParameter("desc", "Auto-created for external map: " + mapName)
                    .setParameter("price", 0.0)
                    .executeUpdate();
                // Retrieve the auto-generated ID
                Object newId = session.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
                cityId = ((Number) newId).intValue();
            }

            // Create external map using native SQL to avoid eager-loading issues
            session.createNativeQuery(
                "INSERT INTO maps (name, description, version, status, price, city_id, map_image) " +
                "VALUES (:name, :desc, :ver, :status, :price, :cityId, :img)")
                .setParameter("name", mapName)
                .setParameter("desc", description != null ? description : "")
                .setParameter("ver", "1.0")
                .setParameter("status", MapStatus.EXTERNAL.name())
                .setParameter("price", 0.0)
                .setParameter("cityId", cityId)
                .setParameter("img", imageBytes)
                .executeUpdate();

            tx.commit();
            session.close();

            System.out.println("Added external map: " + mapName + " in city: " + cityName);
            return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, true);

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            System.err.println("AddExternalMap FAILED: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
        }
    }
}
