package server.repository;

import common.content.Site;

import java.util.List;

public class SiteRepository extends BaseRepository<Site, Integer>
{
    private static SiteRepository instance;
    private SiteRepository() {
        super(Site.class);
    }
    public static synchronized SiteRepository getInstance()
    {
        if (instance == null)
            instance = new SiteRepository();

        return instance;
    }
    public List<Site> findSitesByCityName(String cityName) {
        System.out.println("DEBUG: Searching for sites in city: " + cityName);
        return executeQuery(session ->
                session.createQuery(
                                "SELECT s FROM Site s JOIN s.city c " +
                                        "WHERE c.name = :cityName", Site.class)
                        .setParameter("cityName", cityName)
                        .getResultList()
        );
    }

    /**
     * Search sites by name, description, or location across all cities.
     */
    public List<Site> searchSites(String searchQuery) {
        return executeQuery(session -> {
            String pattern = "%" + searchQuery.toLowerCase() + "%";
            return session.createQuery(
                "SELECT s FROM Site s JOIN FETCH s.city " +
                "WHERE LOWER(s.name) LIKE :pattern " +
                "   OR LOWER(s.description) LIKE :pattern " +
                "   OR LOWER(s.location) LIKE :pattern " +
                "ORDER BY s.city.name, s.name",
                Site.class)
                .setParameter("pattern", pattern)
                .getResultList();
        });
    }
}
