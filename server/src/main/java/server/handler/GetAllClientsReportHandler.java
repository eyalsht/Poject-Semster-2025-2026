package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.report.AllClientsReport;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetAllClientsReportHandler implements RequestHandler {

    private final SessionFactory sessionFactory;

    public GetAllClientsReportHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Message handle(Message request) {

        try (Session session = sessionFactory.openSession()) {

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

            AllClientsReport report = new AllClientsReport(histogram, rows);

            return new Message(ActionType.GET_ALL_CLIENTS_REPORT_RESPONSE, report);
        }
    }
}
