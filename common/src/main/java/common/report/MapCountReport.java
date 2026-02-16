package common.report;

import java.io.Serializable;
import java.util.List;

public class MapCountReport implements Serializable {

    public List<CityRow> rows;

    public MapCountReport(List<CityRow> rows) {
        this.rows = rows;
    }

    public static class CityRow implements Serializable {
        public int cityId;
        public String cityName;

        public long mapsCount;
        public long toursCount;
        public long mapsBoughtCount;
        public long subscriptionsBoughtCount;

        public CityRow(int cityId, String cityName,
                       long mapsCount, long toursCount,
                       long mapsBoughtCount, long subscriptionsBoughtCount) {
            this.cityId = cityId;
            this.cityName = cityName;
            this.mapsCount = mapsCount;
            this.toursCount = toursCount;
            this.mapsBoughtCount = mapsBoughtCount;
            this.subscriptionsBoughtCount = subscriptionsBoughtCount;
        }
    }
}
