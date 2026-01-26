package common.report;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class ActivityReport implements Serializable {
    public LocalDate fromDate;
    public LocalDate toDate;
    public List<CityRow> rows;

    public ActivityReport(LocalDate fromDate, LocalDate toDate, List<CityRow> rows) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.rows = rows;
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
}
