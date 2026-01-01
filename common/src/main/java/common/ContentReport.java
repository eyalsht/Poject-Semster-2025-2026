package common;

import java.time.LocalDate;

public class ContentReport extends Report {

    private int mapsCount;
    private int citiesCount;

    public ContentReport(int reportId, LocalDate startDate, LocalDate endDate,
                         int mapsCount, int citiesCount) {
        super(reportId, startDate, endDate);
        this.mapsCount = mapsCount;
        this.citiesCount = citiesCount;
    }

    public int getMapsCount() { return mapsCount; }
    public void setMapsCount(int mapsCount) { this.mapsCount = mapsCount; }

    public int getCitiesCount() { return citiesCount; }
    public void setCitiesCount(int citiesCount) { this.citiesCount = citiesCount; }
}