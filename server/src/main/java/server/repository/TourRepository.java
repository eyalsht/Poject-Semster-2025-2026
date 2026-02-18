package server.repository;

import common.content.Site;
import common.content.Tour;
import common.enums.MapStatus;

import java.util.List;

public class TourRepository extends BaseRepository<Tour,Integer>{
    private static TourRepository instance;
    private TourRepository() {super(Tour.class);}
    public static synchronized TourRepository getInstance()
    {
        if (instance == null)
            instance = new TourRepository();

        return instance;
    }
    public List<Tour> findToursByCityName(String cityName) {
        System.out.println("DEBUG: Searching for tours sites in city: " + cityName);
        return executeQuery(session ->
                session.createQuery(
                                "SELECT DISTINCT t FROM Tour t " +
                                        "LEFT JOIN FETCH t.sites " +
                                        "JOIN t.city c " +
                                        "WHERE c.name = :cityName", Tour.class)
                        .setParameter("cityName", cityName)
                        .getResultList()
        );
    }

    /**
     * Search tours by name or description across all cities.
     */
    public List<Tour> searchTours(String searchQuery) {
        return executeQuery(session -> {
            String pattern = "%" + searchQuery.toLowerCase() + "%";
            return session.createQuery(
                "SELECT DISTINCT t FROM Tour t " +
                "JOIN FETCH t.city " +
                "LEFT JOIN FETCH t.sites " +
                "WHERE t.city.priceSub > 0 " +
                "AND EXISTS (SELECT 1 FROM GCMMap m WHERE m.city = t.city AND m.status != :extStatus AND m.price > 0 AND m.sites IS NOT EMPTY) " +
                "AND (LOWER(t.name) LIKE :pattern " +
                "   OR LOWER(t.description) LIKE :pattern) " +
                "ORDER BY t.city.name, t.name",
                Tour.class)
                .setParameter("pattern", pattern)
                .setParameter("extStatus", MapStatus.EXTERNAL)
                .getResultList();
        });
    }
}
