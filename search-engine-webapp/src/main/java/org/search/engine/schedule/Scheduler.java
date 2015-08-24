package org.search.engine.schedule;

import or.search.engine.webapp.PerformanceMetricsManager;

import org.apache.log4j.Logger;

import com.wolterskluwer.services.report.service.ReportService;
import com.wolterskluwer.services.report.service.client.ReportServiceClient;

public class Scheduler {

    private static Logger LOG = Logger.getLogger(Scheduler.class);

    private ReportService reportService = ReportServiceClient.createClient();

    public void saveReportingState() {
        LOG.info("Saving reporting state...");
        reportService.saveState();
        LOG.info("Done.");
    }
    
    public void reloadPerformanceMetrics() {
        LOG.info("Reloading performance metrics");
        
        PerformanceMetricsManager.reloadPerformanceMetrics();
    }
}
