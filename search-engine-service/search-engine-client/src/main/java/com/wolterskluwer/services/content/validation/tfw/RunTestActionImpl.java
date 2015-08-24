package com.wolterskluwer.services.content.validation.tfw;

public class RunTestActionImpl {

    /*
    private void processReport(ValidationReport report, ValidationTest test, TestResult result) {
        // the message detail string should contain set of messages separated with a new 
        // line character "\n"
        String detail = report.getDetail();
        List<String> expectedMessagesPool = new ArrayList<String>(test.getExpectedErrorMessages());

        // message detail string
        if (detail != null) {
            String[] messages = detail.split("\n");
            for (String message : messages) {
                if (!message.isEmpty()) {
                    String expectedMessage = test.getExpectedErrorMessage(message);
                    if (expectedMessage != null) {
                        expectedMessagesPool.remove(expectedMessage);
                    } else {
                        result.addErrorMessage(message);
                    }
                }
            }
        }

        if (!expectedMessagesPool.isEmpty()) {
            for (String message : expectedMessagesPool) {
                result.addErrorMessage("No error reported when it was expected: " + message);
            }
        }
    }

    private void testValidateXml(ValidationTest test, TestResult result) {
        Map<String,String> params = test.getInputParameters();

        String fileParamValue = params.get("fileName");
        String schemaParamValue = params.get("schema");

        try {
            
            if (fileParamValue != null && schemaParamValue != null) {
                ValidateXml vXml = ValidationHelper.createValidateXmlRequest(fileParamValue,
                        schemaParamValue);

                ValidateXmlResponse response = clientInstance.getClient().validateXml(vXml);
                processReport(response.getReport(), test, result);
            }

        } catch (Exception ex) {
            result.addErrorMessage(ex.getMessage());
        }
    }

    private void testValidateRdf(ValidationTest test, TestResult result) {
        try {

            Map<String,String> params = test.getInputParameters();
            String fileParamValue = params.get("fileName");
            String ontologyParamValue = params.get("ontology");

            if (fileParamValue != null && ontologyParamValue != null) {
                ValidateRdf vRdf = ValidationHelper.createValidateRdfRequest(fileParamValue,
                        ontologyParamValue);
                ValidateRdfResponse response = clientInstance.getClient().validateRdf(vRdf);
                processReport(response.getReport(), test, result);
            }

        } catch (Exception ex) {
            result.addErrorMessage(ex.getMessage());
        }
    }

    private void testValidateDocument(ValidationTest test, TestResult result) {
        try {

            Map<String,String> params = test.getInputParameters();
            String contentFile = params.get("contentFile");
            String metaFile = params.get("metaFile");
            String relFile = params.get("relFile");

            if (contentFile != null 
                    && metaFile != null
                    && relFile != null) {

                ValidateDocument vDoc = ValidationHelper.createValidateDocumentRequest(contentFile,
                        metaFile, relFile);
                ValidateDocumentResponse response = clientInstance.getClient().validateDocument(vDoc);
                processReport(response.getReport(), test, result);
            }

        } catch (Exception ex) {
            result.addErrorMessage(ex.getMessage());
        }
    }
    */
}
