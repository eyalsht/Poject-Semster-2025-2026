package main.java.common;

import java.time.LocalDate;

public class ActivityReport extends Report {

    private int totalPurchases;
    private int totalRenewals;

    public ActivityReport(int reportId, LocalDate startDate, LocalDate endDate,
                          int totalPurchases, int totalRenewals) {
        super(reportId, startDate, endDate);
        this.totalPurchases = totalPurchases;
        this.totalRenewals = totalRenewals;
    }

    public int getTotalPurchases() { return totalPurchases; }
    public void setTotalPurchases(int totalPurchases) { this.totalPurchases = totalPurchases; }

    public int getTotalRenewals() { return totalRenewals; }
    public void setTotalRenewals(int totalRenewals) { this.totalRenewals = totalRenewals; }
}
