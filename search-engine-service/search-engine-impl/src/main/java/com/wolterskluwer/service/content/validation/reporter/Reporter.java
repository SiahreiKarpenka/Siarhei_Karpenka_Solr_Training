package com.wolterskluwer.service.content.validation.reporter;

import java.util.List;

public interface Reporter {
    void error(String message);

    void warn(String message);

    void info(String message);

    void start(String message);

    void complete(String message);

    public List<String> getErrors();

    public List<String> getWarnings();

    Reporter getResourceReporter(String path);

    void destroy();
}