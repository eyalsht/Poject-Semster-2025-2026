package server.repository;

import common.content.City;
import common.enums.MapStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository for City entity operations.
 */
public class CityRepository extends BaseRepository<City, Integer> {

    private static CityRepository instance;

    private CityRepository() {
        super(City.class);
    }

    public static synchronized CityRepository getInstance() {
        if (instance == null) {
            instance = new CityRepository();
        }
        return instance;
    }

    // ==================== CITY-SPECIFIC QUERIES ====================

    /**
     * Get all cities ordered by name.
     * This replaces DBController.getAllCities()
     */
    @Override
    public List<City> findAll() {
        return executeQuery(session ->
            session.createQuery("FROM City c ORDER BY c.name", City.class)
                   .getResultList()
        );
    }

    /**
     * Find city by name.
     * This replaces DBController.getCityByName()
     */
    public Optional<City> findByName(String cityName) {
        return executeQuery(session ->
            session.createQuery("FROM City c WHERE c.name = :name", City.class)
                   .setParameter("name", cityName)
                   .uniqueResultOptional()
        );
    }

    /**
     * Get all city names ordered alphabetically.
     * This replaces DBController.getAllCityNames()
     */
    public List<String> findAllCityNames() {
        return executeQuery(session ->
            session.createQuery("SELECT c.name FROM City c ORDER BY c.name", String.class)
                   .getResultList()
        );
    }

    /**
     * Get city names visible in the catalog:
     * - priceSub > 0
     * - has at least one non-EXTERNAL map with price > 0
     */
    public List<String> findCatalogCityNames() {
        return executeQuery(session ->
            session.createQuery(
                "SELECT c.name FROM City c " +
                "WHERE c.priceSub > 0 " +
                "AND EXISTS (SELECT 1 FROM GCMMap m WHERE m.city = c AND m.status != :extStatus AND m.price > 0) " +
                "ORDER BY c.name", String.class)
                   .setParameter("extStatus", MapStatus.EXTERNAL)
                   .getResultList()
        );
    }

    /**
     * Get city with all its maps loaded (eager fetch).
     */
    public Optional<City> findByIdWithMaps(int cityId) {
        return executeQuery(session ->
            session.createQuery(
                "FROM City c LEFT JOIN FETCH c.maps WHERE c.id = :id", City.class)
                   .setParameter("id", cityId)
                   .uniqueResultOptional()
        );
    }

    /**
     * Get city with all relationships loaded.
     */
    public Optional<City> findByIdWithAll(int cityId) {
        return executeQuery(session ->
            session.createQuery(
                "FROM City c " +
                "LEFT JOIN FETCH c.maps " +
                "LEFT JOIN FETCH c.sites " +
                "LEFT JOIN FETCH c.tours " +
                "WHERE c.id = :id", City.class)
                   .setParameter("id", cityId)
                   .uniqueResultOptional()
        );
    }

    /**
     * Search cities with counts - returns Object[] containing:
     * [0] = City, [1] = mapCount (Long), [2] = siteCount (Long), [3] = tourCount (Long)
     */
    public List<Object[]> searchWithCounts(String searchQuery) {
        return executeQuery(session -> {
            String pattern = "%" + searchQuery.toLowerCase() + "%";
            String hql = """
                SELECT DISTINCT c,
                       (SELECT COUNT(m) FROM GCMMap m WHERE m.city = c AND m.status != :extStatus AND m.price > 0),
                       (SELECT COUNT(s) FROM Site s WHERE s.city = c),
                       (SELECT COUNT(t) FROM Tour t WHERE t.city = c)
                FROM City c
                LEFT JOIN c.sites s
                LEFT JOIN c.tours t
                WHERE c.priceSub > 0
                  AND EXISTS (SELECT 1 FROM GCMMap m2 WHERE m2.city = c AND m2.status != :extStatus AND m2.price > 0)
                  AND (LOWER(c.name) LIKE :pattern
                   OR LOWER(c.description) LIKE :pattern
                   OR LOWER(s.name) LIKE :pattern
                   OR LOWER(s.description) LIKE :pattern
                   OR LOWER(t.name) LIKE :pattern
                   OR LOWER(t.description) LIKE :pattern)
                ORDER BY c.name
                """;
            return session.createQuery(hql, Object[].class)
                .setParameter("pattern", pattern)
                .setParameter("extStatus", MapStatus.EXTERNAL)
                .getResultList();
        });
    }

    /**
     * Lightweight query to get city name and subscription price without loading
     * the full entity graph (maps, sites, tours).
     * Returns Object[] {name (String), priceSub (Double)} or null if not found.
     */
    public Object[] findNameAndPrice(int cityId) {
        return executeQuery(session -> {
            Object[] result = (Object[]) session.createNativeQuery(
                "SELECT name, price_sub FROM cities WHERE id = :id")
                .setParameter("id", cityId)
                .getSingleResultOrNull();
            return result;
        });
    }

    /**
     * Get map descriptions for a given city.
     */
    public List<String> getMapDescriptionsForCity(int cityId) {
        return executeQuery(session -> {
            String hql = "SELECT m.description FROM GCMMap m WHERE m.city.id = :cityId";
            return session.createQuery(hql, String.class)
                .setParameter("cityId", cityId)
                .getResultList();
        });
    }
}
