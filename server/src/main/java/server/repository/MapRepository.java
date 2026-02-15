package server.repository;

import common.content.GCMMap;
import common.enums.MapStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GCMMap entity operations.
 */
public class MapRepository extends BaseRepository<GCMMap, Integer> {

    // Singleton instance
    private static MapRepository instance;

    private MapRepository() {
        super(GCMMap.class);
    }

    public static synchronized MapRepository getInstance() {
        if (instance == null) {
            instance = new MapRepository();
        }
        return instance;
    }

    // ==================== MAP-SPECIFIC QUERIES ====================

    /**
     * Get map names for a specific city (catalog-visible maps only).
     * This replaces DBController.getMapNamesForCity()
     */
    public List<String> findMapNamesByCity(String cityName) {
        return executeQuery(session ->
            session.createQuery(
                "SELECT DISTINCT m.name FROM GCMMap m " +
                "WHERE m.city.name = :cityName " +
                "AND m.status != :extStatus AND m.price > 0 " +
                "ORDER BY m.name", String.class)
                   .setParameter("cityName", cityName)
                   .setParameter("extStatus", MapStatus.EXTERNAL)
                   .getResultList()
        );
    }

    /**
     * Get versions for a specific city and map name (catalog-visible maps only).
     * This replaces DBController.getVersionsForCityMap()
     */
    public List<String> findVersionsByCityAndMapName(String cityName, String mapName) {
        return executeQuery(session ->
            session.createQuery(
                "SELECT DISTINCT m.version FROM GCMMap m " +
                "WHERE m.city.name = :cityName AND m.name = :mapName " +
                "AND m.status != :extStatus AND m.price > 0 " +
                "ORDER BY m.version", String.class)
                   .setParameter("cityName", cityName)
                   .setParameter("mapName", mapName)
                   .setParameter("extStatus", MapStatus.EXTERNAL)
                   .getResultList()
        );
    }

    /**
     * Find maps by criteria (city, name, version).
     * Filters out EXTERNAL maps and maps with price=0 (not yet priced).
     * This replaces DBController.getCatalogRows() - returns actual entities!
     */
    public List<GCMMap> findByCriteria(String cityName, String mapName, String version) {
        return executeQuery(session -> {
            StringBuilder hql = new StringBuilder("FROM GCMMap m JOIN FETCH m.city WHERE 1=1");
            hql.append(" AND m.status != :extStatus AND m.price > 0");
            hql.append(" AND m.city.priceSub > 0");

            if (cityName != null && !cityName.isBlank()) {
                hql.append(" AND m.city.name = :cityName");
            }
            if (mapName != null && !mapName.isBlank()) {
                hql.append(" AND m.name = :mapName");
            }
            if (version != null && !version.isBlank()) {
                hql.append(" AND m.version = :version");
            }
            hql.append(" ORDER BY m.city.name, m.name, m.version");

            var query = session.createQuery(hql.toString(), GCMMap.class);
            query.setParameter("extStatus", MapStatus.EXTERNAL);

            if (cityName != null && !cityName.isBlank()) {
                query.setParameter("cityName", cityName);
            }
            if (mapName != null && !mapName.isBlank()) {
                query.setParameter("mapName", mapName);
            }
            if (version != null && !version.isBlank()) {
                query.setParameter("version", version);
            }

            return query.getResultList();
        });
    }

    /**
     * Find a specific map by city name, map name, and version.
     * This replaces DBController.getMapId() - returns the whole entity!
     */
    public Optional<GCMMap> findByCityNameAndVersion(String cityName, String mapName, String version) {
        return executeQuery(session ->
            session.createQuery(
                "FROM GCMMap m " +
                "WHERE m.city.name = :cityName AND m.name = :mapName AND m.version = :version",
                GCMMap.class)
                   .setParameter("cityName", cityName)
                   .setParameter("mapName", mapName)
                   .setParameter("version", version)
                   .uniqueResultOptional()
        );
    }

    /**
     * Get map with sites loaded.
     */
    public Optional<GCMMap> findByIdWithSites(int mapId) {
        return executeQuery(session ->
            session.createQuery(
                "FROM GCMMap m LEFT JOIN FETCH m.sites WHERE m.id = :id",
                GCMMap.class)
                   .setParameter("id", mapId)
                   .uniqueResultOptional()
        );
    }

    /**
     * Update map price.
     */
    public void updatePrice(int mapId, double newPrice) {
        executeInTransaction(session -> {
            GCMMap map = session.get(GCMMap.class, mapId);
            if (map != null) {
                map.setPrice(newPrice);
            }
        });
    }

    /**
     * Find all maps with EXTERNAL status (the simulated external repository).
     * Returns lightweight results (no blobs loaded via default query).
     */
    public List<GCMMap> findExternalMaps() {
        return executeQuery(session ->
            session.createQuery(
                "FROM GCMMap m JOIN FETCH m.city WHERE m.status = :extStatus " +
                "ORDER BY m.city.name, m.name",
                GCMMap.class)
                   .setParameter("extStatus", MapStatus.EXTERNAL)
                   .getResultList()
        );
    }
}
