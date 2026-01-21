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
        return executeQuery(session ->
                session.createQuery(
                                "SELECT s FROM Site s " +
                                        "WHERE s.city.name = :name", Site.class)
                        .setParameter("name", cityName)
                        .getResultList()
        );
    }
}
