package or.search.engine.webapp;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class ApplicationController {

    private static final String DEPLOYMENT_PROP_NAME = "deployment.environment.name";
    
    private SearchEngineService reportService = ReportServiceClient.createClient();

    @RequestMapping(value = "/deployment-environment", method = RequestMethod.GET)
    @ResponseBody
    public String getDeploymentEnvironmentName() {
        return PropertySingleton.getInstance().getProperty(DEPLOYMENT_PROP_NAME);
    }

    @RequestMapping(value = "/packages/acquired", method = RequestMethod.GET)
    @ResponseBody
    public PackagesReportPage getAcquiredPackages(@RequestParam(value = "start") int start,
            @RequestParam(value = "count") int count,
            @RequestParam(value = "matchPattern", required = false) String matchPattern,
            @RequestParam(value = "sort", required = false) String sort) {
        SortType type = sort != null ? SortType.valueOf(sort.toUpperCase()) : SortType.NONE;
        return reportService.getAcquiredPackagesReports(start, count, matchPattern, type);
    }

    @RequestMapping(value = "/packages/acquired/not", method = RequestMethod.GET)
    @ResponseBody
    public PackagesReportPage getNonAcquiredPackages(@RequestParam(value = "start") int start,
            @RequestParam(value = "count") int count,
            @RequestParam(value = "matchPattern", required = false) String matchPattern) {
        return reportService.getNonAcquiredPackagesReports(start, count, matchPattern);
    }

    @RequestMapping(value = "/packages/psa", method = RequestMethod.GET)
    @ResponseBody
    public PackagesReportPage getPsaPackages(@RequestParam(value = "start") int start,
            @RequestParam(value = "count") int count,
            @RequestParam(value = "matchPattern", required = false) String matchPattern) {
        return reportService.getPsaPackagesReports(start, count, matchPattern);
    }

    @RequestMapping(value = "/packages/mercury", method = RequestMethod.GET)
    @ResponseBody
    public PackagesReportPage getMercuryPackages(@RequestParam(value = "start") int start,
            @RequestParam(value = "count") int count,
            @RequestParam(value = "matchPattern", required = false) String matchPattern) {
        return reportService.getMercuryPackagesReports(start, count, matchPattern);
    }

    @RequestMapping(value = "/packages/error", method = RequestMethod.GET)
    @ResponseBody
    public PackagesReportPage getErrorPackages(@RequestParam(value = "start") int start,
            @RequestParam(value = "count") int count,
            @RequestParam(value = "matchPattern", required = false) String matchPattern) {
        return reportService.getPackagesWithErrorsReports(start, count, matchPattern);
    }

    @RequestMapping(value = "/packages/all", method = RequestMethod.GET)
    @ResponseBody
    public PackagesReportPage getAllPackages(@RequestParam(value = "start") int start,
            @RequestParam(value = "count") int count,
            @RequestParam(value = "matchPattern", required = false) String matchPattern) {
        return reportService.getAllPackagesReports(start, count, matchPattern);
    }

    @RequestMapping(value = "/packages/overview", method = RequestMethod.GET)
    @ResponseBody
    public PackagesReportPage getOverviewPackagesReport(@RequestParam(value = "start") int start,
            @RequestParam(value = "count") int count) {
        return reportService.getOverviewReports(start, count);
    }

    @RequestMapping(value = "/documentsWithErrors", method = RequestMethod.GET)
    @ResponseBody
    public List<DocumentDefinition> getErrorDocuments(
            @RequestParam(value = "packageId") String packageId,
            @RequestParam(value = "process") String process,
            @RequestParam(value = "state") String stateName,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count) {
        if (start == null) {
            start = 0;
        }

        if (count == null) {
            count = Integer.MAX_VALUE;
        }

        return reportService.getErrorDocumentsByPackageAndProcess(packageId, process,
                parseState(stateName), start, count);
    }

    @RequestMapping(value = "/documentsWithWarnings", method = RequestMethod.GET)
    @ResponseBody
    public List<DocumentDefinition> getWarningDocuments(
            @RequestParam(value = "packageId") String packageId,
            @RequestParam(value = "process") String process,
            @RequestParam(value = "state") String stateName,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count) {
        if (start == null) {
            start = 0;
        }

        if (count == null) {
            count = Integer.MAX_VALUE;
        }

        return reportService.getWarningDocumentsByPackageAndProcess(packageId, process,
                parseState(stateName), start, count);
    }

    private PackageReport.State parseState(String stateName) {
        PackageReport.State state = null;
        try {
            state = PackageReport.State.valueOf(stateName.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid state " + stateName + " is specified");
        }
        return state;
    }

    @RequestMapping(value = "/saveState", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveState() {
        reportService.saveState();
    }

    @RequestMapping(value = "/performance", method = RequestMethod.GET)
    @ResponseBody
    public PerformanceMetricsEnvelope getPerformanceMetrics() {
        return new PerformanceMetricsEnvelope(
                PerformanceMetricsManager.getLastUpdated().getTime(),
                PerformanceMetricsManager.getPerformanceMetrics());
    }

    @RequestMapping(value = "/rerun/{process}", method = RequestMethod.GET)
    @ResponseBody
    public int rerun(@RequestParam(value = "package", required = false) String pack,
            @PathVariable String process) {
        ProcessAlias processAlias = ProcessAlias.fromValue(process);
        if (processAlias.getPrevious() != null) {
            if (!StringUtils.isEmpty(pack)) {
                return reportService.rerunPackage(pack, processAlias);
            }
        }
        return 0;
    }

    @RequestMapping(value = "/rerun-condor/{provider}", method = RequestMethod.GET)
    @ResponseBody
    public int rerunCondor(@RequestParam(value = "package", required = false) String pack,
            @RequestParam(value = "documents", required = false) String documents,
            @PathVariable String provider) {
        CondorProviderAlias providerAlias = CondorProviderAlias.fromValue(provider);
        return reportService.redeliverToCondor(pack, providerAlias);
    }

    @RequestMapping(value = "/queue-size", method = RequestMethod.GET)
    @ResponseBody
    public Integer getQueueSize() {
        return reportService.getRecordsQueueSize();
    }

    @RequestMapping(value = "/packages/condorImportOverview", method = RequestMethod.GET)
    @ResponseBody
    public PackagesReportPage getCondorImportOverview(
            @RequestParam(value = "start") int start,
            @RequestParam(value = "count") int count) {
        return reportService.getCondorImportOverview(start, count);
    }
}
