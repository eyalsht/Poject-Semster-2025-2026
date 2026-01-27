package server.report.services;

import common.enums.ReportType;
import common.report.AllClientsReport;
import org.hibernate.Session;
import server.report.ReportManager;
import server.report.ReportRequestContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClientsReportService implements ReportManager.ParamAwareReportService {

    @Override
    public ReportType getType() {
        return ReportType.CLIENTS;
    }

    /**
     * params: none for now
     */
    @Override
    public Object generate(ReportRequestContext ctx, Object... params) {

        try (Session session = ctx.getSessionFactory().openSession()) {

            List<AllClientsReport.ClientRow> rows = session.createQuery("""
                select new common.report.AllClientsReport$ClientRow(
                    c.id, c.username, c.email, c.firstName, c.lastName, c.createdAt
                )
                from Client c
                order by c.id desc
            """, AllClientsReport.ClientRow.class).getResultList();

            LocalDateTime from = LocalDate.now()
                    .withDayOfMonth(1)
                    .minusMonths(4)
                    .atStartOfDay();

            List<LocalDateTime> createdAts = session.createQuery("""
                select c.createdAt
                from Client c
                where c.createdAt >= :from
            """, LocalDateTime.class)
                    .setParameter("from", from)
                    .getResultList();

            Map<YearMonth, Long> counts = new LinkedHashMap<>();
            YearMonth start = YearMonth.now().minusMonths(4);
            for (int i = 0; i < 5; i++) counts.put(start.plusMonths(i), 0L);

            for (LocalDateTime dt : createdAts) {
                YearMonth ym = YearMonth.from(dt);
                if (counts.containsKey(ym)) counts.put(ym, counts.get(ym) + 1);
            }

            List<AllClientsReport.MonthCount> histogram = counts.entrySet().stream()
                    .map(e -> new AllClientsReport.MonthCount(e.getKey(), e.getValue()))
                    .toList();

            return new AllClientsReport(histogram, rows);
        }
    }
}
