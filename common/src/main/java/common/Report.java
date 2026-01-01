package main.java.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class Report {

    protected int reportId;
    protected LocalDateTime producedDate;
    protected LocalDate startDate;
    protected LocalDate endDate;

    public Report(int reportId, LocalDate startDate, LocalDate endDate) {
        this.reportId = reportId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.producedDate = null; // will be set when produceReport() is called
    }

    public void produceReport() {
        // mark when the report was produced
        this.producedDate = LocalDateTime.now();
    }

    public void exportToPDF() {
        // placeholder: real PDF export would require a library
        // (you can leave empty if assignment doesn't require actual PDF)
    }

    // getters (optional but useful)
    public int getReportId() { return reportId; }
    public LocalDateTime getProducedDate() { return producedDate; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}