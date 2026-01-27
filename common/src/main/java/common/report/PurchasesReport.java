package common.report;

import java.io.Serializable;
import java.time.LocalDate;

public class PurchasesReport implements Serializable {
    public final LocalDate from;
    public final LocalDate to;
    public final int cityId;
    public final String cityName;

    public final int oneTime;
    public final int subscriptions;
    public final int renewals;

    public PurchasesReport(LocalDate from, LocalDate to, int cityId, String cityName,
                           int oneTime, int subscriptions, int renewals) {
        this.from = from;
        this.to = to;
        this.cityId = cityId;
        this.cityName = cityName;
        this.oneTime = oneTime;
        this.subscriptions = subscriptions;
        this.renewals = renewals;
    }
}
