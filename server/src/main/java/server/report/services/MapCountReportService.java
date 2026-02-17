package server.report.services;

import common.enums.ReportType;
import common.report.MapCountReport;
import org.hibernate.Session;
import server.report.ReportManager;
import server.report.ReportRequestContext;

import java.util.List;

public class MapCountReportService implements ReportManager.ParamAwareReportService {

    @Override
    public ReportType getType() {
        return ReportType.MAP_COUNT;
    }

    @Override
    public Object generate(ReportRequestContext ctx, Object... params) {

        try (Session s = ctx.getSessionFactory().openSession()) {

            List<MapCountReport.CityRow> rows = s.createQuery("""
                select new common.report.MapCountReport$CityRow(
                    c.id,
                    c.name,
                    (select count(m.id) from GCMMap m where m.city.id = c.id and m.status <> common.enums.MapStatus.EXTERNAL),
                    (select count(t.id) from Tour t where t.city.id = c.id),
                    (select count(otp.id) from OneTimePurchase otp where otp.city.id = c.id),
                    (select count(sub.id) from Subscription sub where sub.city.id = c.id and sub.isRenewal = false)
                )
                from City c
                order by c.name
            """, MapCountReport.CityRow.class).getResultList();

            return new MapCountReport(rows);
        }
    }
}
