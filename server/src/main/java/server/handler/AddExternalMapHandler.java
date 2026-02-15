package server.handler;

import common.content.City;
import common.content.GCMMap;
import common.enums.ActionType;
import common.enums.MapStatus;
import common.messaging.Message;
import org.hibernate.Session;
import server.HibernateUtil;

import java.util.List;

/**
 * Temporary handler for adding maps to the simulated external repository.
 * Receives a List containing: [mapName, description, cityName, imageBytes].
 */
public class AddExternalMapHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> params = (List<Object>) request.getMessage();

            String mapName = (String) params.get(0);
            String description = (String) params.get(1);
            String cityName = (String) params.get(2);
            byte[] imageBytes = (byte[]) params.get(3);

            if (mapName == null || mapName.isBlank() || cityName == null || cityName.isBlank()) {
                System.err.println("AddExternalMap: mapName or cityName is blank");
                return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
            }

            if (imageBytes == null || imageBytes.length == 0) {
                System.err.println("AddExternalMap: imageBytes is null or empty");
                return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
            }

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                session.beginTransaction();

                // Use a lightweight query to find city ID by name (avoids loading eager collections)
                Integer cityId = (Integer) session.createNativeQuery(
                        "SELECT id FROM cities WHERE name = :name")
                        .setParameter("name", cityName)
                        .uniqueResult();

                City city;
                if (cityId != null) {
                    // Use getReference to avoid loading the full entity graph
                    city = session.getReference(City.class, cityId);
                } else {
                    city = new City();
                    city.setName(cityName);
                    city.setDescription("Auto-created for external map: " + mapName);
                    city.setPriceSub(0);
                    session.persist(city);
                    session.flush();
                }

                // Create external map
                GCMMap newMap = new GCMMap();
                newMap.setName(mapName);
                newMap.setDescription(description != null ? description : "");
                newMap.setVersion("1.0");
                newMap.setStatus(MapStatus.EXTERNAL);
                newMap.setPrice(0.0);
                newMap.setCity(city);
                newMap.setMapImage(imageBytes);

                session.persist(newMap);
                session.getTransaction().commit();

                System.out.println("Added external map: " + mapName + " in city: " + cityName);
                return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, true);
            }

        } catch (Exception e) {
            System.err.println("AddExternalMap FAILED: " + e.getMessage());
            e.printStackTrace();
            return new Message(ActionType.ADD_EXTERNAL_MAP_RESPONSE, false);
        }
    }
}
