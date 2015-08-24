package or.search.engine.webapp;

import java.util.List;

import com.wolterskluwer.services.report.service.dto.ProcessPerformanceMetric;

public class PerformanceMetricsEnvelope {

    private long lastUpdated;

    private List<ProcessPerformanceMetric> metrics;

    public PerformanceMetricsEnvelope() {
    }

    public PerformanceMetricsEnvelope(long lastUpdated,
            List<ProcessPerformanceMetric> metrics) {
        this.lastUpdated = lastUpdated;
        this.metrics = metrics;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<ProcessPerformanceMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<ProcessPerformanceMetric> metrics) {
        this.metrics = metrics;
    }
}
