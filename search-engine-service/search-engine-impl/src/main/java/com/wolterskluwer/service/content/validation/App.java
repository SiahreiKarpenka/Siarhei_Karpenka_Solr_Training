package com.wolterskluwer.service.content.validation;

import org.apache.log4j.Logger;

import com.wolterskluwer.framework.async.task.TaskService;
import com.wolterskluwer.service.config.Config;
import com.wolterskluwer.service.discovery.api.PlanArtifact;
import com.wolterskluwer.service.discovery.api.PlanArtifactManager;
import com.wolterskluwer.service.discovery.util.PlanAddress;

/**
 * This class is introduced in order to provide an access to every
 * object that should be available on the application level.
 *
 *
 */
public final class App {

    public static final String SERVICE_NAME = "validation";

    private static final TaskService<ValidationResult> taskService =
            TaskService.newTaskService(getMaxNumberOfAsyncThreads()); 

    private static int getMaxNumberOfAsyncThreads() {
        String configurationValue = Config.getProperty("service.async.maxThreads", "1");
        return Integer.parseInt(configurationValue);
    }

    private static final PlanArtifactManager planArtifactManager = createPlanArtifactManager();
    
    private static Logger log = Logger.getLogger(App.class);

    private static PlanArtifactManager createPlanArtifactManager() {
//        String discoveryEndpointURL = Config.getProperty(DISCOVERY_ENDPOINT_URL);
//        String username = Config.getProperty(DISCOVERY_USERNAME);
//        String password = Config.getProperty(DISCOVERY_PASSWORD);
//        if (username == null || username.isEmpty()) {
//            return PlanArtifactManager.fromDiscoveryEndpointURL(discoveryEndpointURL);
//        }
        return PlanArtifactManager.fromDiscoveryClient();
    }

	public static ValidationServiceConfiguration getConfiguration(PlanAddress planAddress) {
        PlanArtifact planArtifact = null;
        try {
            planArtifact = planArtifactManager.getPlanArtifact(
                    planAddress.getServiceName(),
                    planAddress.getPlanName(),
                    planAddress.getPlanVersion());
        } catch (Exception e){
            log.error(e.getMessage());
            throw new RuntimeException("Can't get plan: " + planAddress.toString());
        }
        return new ValidationServiceConfiguration(planArtifact);
    }

    public static TaskService<ValidationResult> getTaskService() {
        return taskService;
    }

}
