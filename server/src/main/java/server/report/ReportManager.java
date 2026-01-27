package server.report;

import common.enums.ReportType;
import org.hibernate.SessionFactory;
import server.report.services.ActivityReportService;
import server.report.services.ClientsReportService;

import java.util.EnumMap;
import java.util.Map;

public class ReportManager
{

    private final Map<ReportType, ParamAwareReportService> services = new EnumMap<>(ReportType.class);
    private final ReportRequestContext ctx;

    public ReportManager(SessionFactory sf)
    {
        this.ctx = new ReportRequestContext(sf);

        // 🔥 one place to rule them all
        register(new ActivityReportService());
        register(new ClientsReportService());
    }

    private void register(ParamAwareReportService service) {
        services.put(service.getType(), service);
    }

    public <T> T generate(ReportType type, Class<T> expectedClass, Object... params) {
        ParamAwareReportService service = services.get(type);
        if (service == null)
            throw new IllegalArgumentException("Unknown report type: " + type);

        Object out = service.generate(ctx, params);
        return expectedClass.cast(out);
    }

    public void refreshDailyReports() {
        for (ParamAwareReportService s : services.values()) {
            try {
                s.refreshDaily(ctx); // all services refresh
            } catch (Exception e) {
                System.err.println("[ReportManager] refreshDaily failed for " + s.getType());
                e.printStackTrace();
            }
        }
    }


    // internal helper
    public interface ParamAwareReportService extends ReportService {
        Object generate(ReportRequestContext ctx, Object... params);
    }
}
