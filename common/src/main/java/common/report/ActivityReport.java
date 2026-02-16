package common.report;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class ActivityReport implements Serializable {

    public LocalDate fromDate;
    public LocalDate toDate;

    // Chart (business metrics)
    public int maps;
    public int oneTimePurchases;
    public int subscriptions;
    public int renewals;

    // City-enter views count (map_id is null)
    public int cityEnterViewsTotal;

    // For combo display: cityId, cityName, cityEnterViews
    public List<CityEnterRow> cityEnterRows;

    // Table (always): maps rows
    public List<MapRow> mapRows;

    public ActivityReport(LocalDate fromDate, LocalDate toDate,
                          int maps, int oneTimePurchases, int subscriptions, int renewals,
                          int cityEnterViewsTotal,
                          List<CityEnterRow> cityEnterRows,
                          List<MapRow> mapRows) {

        this.fromDate = fromDate;
        this.toDate = toDate;
        this.maps = maps;
        this.oneTimePurchases = oneTimePurchases;
        this.subscriptions = subscriptions;
        this.renewals = renewals;
        this.cityEnterViewsTotal = cityEnterViewsTotal;
        this.cityEnterRows = cityEnterRows;
        this.mapRows = mapRows;
    }

    public static class CityEnterRow implements Serializable {
        public int cityId;
        public String cityName;
        public int cityEnterViews;

        public CityEnterRow(int cityId, String cityName, int cityEnterViews) {
            this.cityId = cityId;
            this.cityName = cityName;
            this.cityEnterViews = cityEnterViews;
        }
    }

    public static class MapRow implements Serializable {
        public int mapId;
        public String mapName;
        public int cityId;
        public String cityName;
        public int downloads;
        public int views;

        public MapRow(int mapId, String mapName, int cityId, String cityName, int downloads, int views) {
            this.mapId = mapId;
            this.mapName = mapName;
            this.cityId = cityId;
            this.cityName = cityName;
            this.downloads = downloads;
            this.views = views;
        }
    }
}
