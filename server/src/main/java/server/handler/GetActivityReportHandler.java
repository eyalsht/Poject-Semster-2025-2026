package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.report.ActivityReport;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.time.LocalDate;
import java.util.*;

public class GetActivityReportHandler implements RequestHandler {

    private final SessionFactory sf;
    public GetActivityReportHandler(SessionFactory sf) { this.sf = sf; }

    @Override
    public Message handle(Message request) {

        ArrayList<Object> payload = (ArrayList<Object>) request.getMessage();
        LocalDate from = (LocalDate) payload.get(0);
        LocalDate to = (LocalDate) payload.get(1);
        Integer cityId = (Integer) payload.get(2); // for now always not-null

        try (Session s = sf.openSession()) {
            s.beginTransaction();

            // Sum daily stats in range
            List<Object[]> stats = s.createNativeQuery("""
                SELECT c.id, c.name,
                       COALESCE(SUM(d.one_time_purchases),0),
                       COALESCE(SUM(d.subscriptions),0),
                       COALESCE(SUM(d.subscription_renewals),0),
                       COALESCE(SUM(d.views),0),
                       COALESCE(SUM(d.downloads),0)
                FROM cities c
                LEFT JOIN daily_city_activity_stats d
                       ON d.city_id = c.id
                      AND d.stat_date >= :fromDate
                      AND d.stat_date <= :toDate
                WHERE (:cityId IS NULL OR c.id = :cityId)
                GROUP BY c.id, c.name
                ORDER BY c.name
            """)
                    .setParameter("fromDate", java.sql.Date.valueOf(from))
                    .setParameter("toDate", java.sql.Date.valueOf(to))
                    .setParameter("cityId", cityId)
                    .getResultList();

            // Maps count (current)
            Map<Integer, Integer> mapsCount = new HashMap<>();
            List<Object[]> maps = s.createNativeQuery("""
                SELECT m.city_id, COUNT(*)
                FROM maps m
                GROUP BY m.city_id
            """).getResultList();

            for (Object[] r : maps) {
                mapsCount.put(((Number) r[0]).intValue(), ((Number) r[1]).intValue());
            }

            List<ActivityReport.CityRow> rows = new ArrayList<>();
            for (Object[] r : stats) {
                int cid = ((Number) r[0]).intValue();
                String cname = (String) r[1];
                int oneTime = ((Number) r[2]).intValue();
                int subs = ((Number) r[3]).intValue();
                int renew = ((Number) r[4]).intValue();
                int views = ((Number) r[5]).intValue();
                int downloads = ((Number) r[6]).intValue();

                int mapsN = mapsCount.getOrDefault(cid, 0);

                rows.add(new ActivityReport.CityRow(cid, cname, mapsN, oneTime, subs, renew, views, downloads));
            }

            s.getTransaction().commit();

            ActivityReport report = new ActivityReport(from, to, rows);
            return new Message(ActionType.GET_ACTIVITY_REPORT_RESPONSE, report);
        }
    }
}
