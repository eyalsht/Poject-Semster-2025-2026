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
}
