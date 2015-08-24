package com.wolterskluwer.services.content.validation.tfw;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wolterskluwer.services.library.testing.TestSuiteRunner;
import com.wolterskluwer.services.library.testing.beans.AbstractTestSuite;

public class ClientRunnerTestSuite {

    private static final String TEMPLATE_LOCATION = "conf/test-result-template.xhtml",
            TEST_DESCRIPTOR = "validation-test.xml", REPORT_LOCATION = "report.xhtml";

    private static final Logger log = LoggerFactory.getLogger(ClientRunnerTestSuite.class);

    public static void main(String[] args) {
        String packageURL = args.length > 0 ? args[0] : null;
        try {
            validate(packageURL);
        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }
    }

    private static void validate(String packageURL) throws Exception {
        TestSuiteRunner runner = new TestSuiteRunner();
        runner.getResultReporter().setReportLocation(REPORT_LOCATION);
        runner.getResultReporter().setTemplateLocation(TEMPLATE_LOCATION);
        AbstractTestSuite suite = new ValidationTestSuite();

        if (packageURL != null && !packageURL.isEmpty()) {
            ValidationTest test = new ValidationTest(
                    "Validate Package",
                    "Validates the given package.",
                    packageURL,
                    "http://services.wolterskluwer.com/ceres/service/configuration/content/condor/0.0.2-SNAPSHOT/condor");
            suite.addTest(test);
        } else {
            log.warn("Package was not provided for validation. Will use " + TEST_DESCRIPTOR
                    + " to get the test data");
            File testDescriptor = new File(TEST_DESCRIPTOR);
            if (!testDescriptor.exists() || !testDescriptor.isFile()) {
                throw new Exception("Could not find file " + TEST_DESCRIPTOR);
            }
            try {
                suite.loadSuite(testDescriptor);
            } catch (Exception e) {
                throw new Exception("Could not parse file " + TEST_DESCRIPTOR, e);
            }
        }
        runner.addSuite(suite);
        runner.runTests();
    }
}
