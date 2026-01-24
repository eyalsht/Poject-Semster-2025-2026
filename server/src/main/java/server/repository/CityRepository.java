package server.repository;

import common.content.City;

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
}
