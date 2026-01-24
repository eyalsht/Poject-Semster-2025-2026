package server.repository;

import common.content.Site;
import common.content.Tour;

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
                                "SELECT t FROM Tour t JOIN t.city c " +
                                        "WHERE c.name = :cityName", Tour.class)
                        .setParameter("cityName", cityName)
                        .getResultList()
        );
    }
}
