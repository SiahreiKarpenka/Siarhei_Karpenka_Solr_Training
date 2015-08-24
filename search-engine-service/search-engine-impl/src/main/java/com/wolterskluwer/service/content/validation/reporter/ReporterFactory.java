package com.wolterskluwer.service.content.validation.reporter;

public class ReporterFactory {

    public static UpdatableReporter newReporter(String id) {
        return new SimpleReporter();
    }
}
