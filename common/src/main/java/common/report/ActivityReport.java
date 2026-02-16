package common.report;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class ActivityReport implements Serializable {
    public LocalDate fromDate;
    public LocalDate toDate;

    // Always returned (city aggregation)
    public List<CityRow> rows;

    // Returned when a SINGLE city is requested (cityId != null)
    public Integer tableCityId;
    public String tableCityName;
    public List<MapRow> mapRows;

    public ActivityReport(LocalDate fromDate,
                          LocalDate toDate,
                          List<CityRow> rows,
                          Integer tableCityId,
                          String tableCityName,
                          List<MapRow> mapRows) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.rows = rows;
        this.tableCityId = tableCityId;
        this.tableCityName = tableCityName;
        this.mapRows = mapRows;
    }

    public static class CityRow implements Serializable {
        public int cityId;
        public String cityName;

        public int maps;
        public int oneTimePurchases;
        public int subscriptions;
        public int renewals;
        public int views;
        public int downloads;

        public CityRow(int cityId, String cityName, int maps, int oneTimePurchases,
                       int subscriptions, int renewals, int views, int downloads) {
            this.cityId = cityId;
            this.cityName = cityName;
            this.maps = maps;
            this.oneTimePurchases = oneTimePurchases;
            this.subscriptions = subscriptions;
            this.renewals = renewals;
            this.views = views;
            this.downloads = downloads;
        }
    }

    public static class MapRow implements Serializable {
        public int mapId;
        public String mapName;
        public int views;
        public int downloads;

        public MapRow(int mapId, String mapName, int views, int downloads) {
            this.mapId = mapId;
            this.mapName = mapName;
            this.views = views;
            this.downloads = downloads;
        }
    }
}
