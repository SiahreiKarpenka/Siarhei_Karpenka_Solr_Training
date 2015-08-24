package or.search.engine.webapp;

import java.util.Date;
import java.util.List;

import com.wolterskluwer.services.report.service.ReportService;
import com.wolterskluwer.services.report.service.client.ReportServiceClient;
import com.wolterskluwer.services.report.service.dto.ProcessPerformanceMetric;

public class PerformanceMetricsManager {

    private static ReportService reportService = ReportServiceClient.createClient();
    
    private static List<ProcessPerformanceMetric> currentPerformanceMetrics;
    
    private static Date lastUpdated = new Date();
    
    public static List<ProcessPerformanceMetric> getPerformanceMetrics() {
        if (currentPerformanceMetrics == null) {
            reloadPerformanceMetrics();
        }

        return currentPerformanceMetrics;
    }
    
    public static Date getLastUpdated() {
        return lastUpdated;
    }
    
    /**
     * To be called by scheduled update job
     */
    public static void reloadPerformanceMetrics() {
        currentPerformanceMetrics = reportService.getPerformanceMetrics();
        lastUpdated = new Date();
    }
}
