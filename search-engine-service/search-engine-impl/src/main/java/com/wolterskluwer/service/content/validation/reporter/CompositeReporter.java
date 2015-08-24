package com.wolterskluwer.service.content.validation.reporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <code>CompositeReporter</code> keeps a collection of <code>Reporter</code>s and delegates all 
 * method invocations to them. Instances of <code>CompositeReporter</code> class are not expected 
 * to have their own reporting logic, they only containers that delegate all the method calls to
 * their child <code>Reporter</code> objects.
 * 
 * @TODO Should CompositeReporter class have getters for error, info and warning messages?
 */
public class CompositeReporter implements UpdatableReporter {

    private List<UpdatableReporter> reporters = new ArrayList<UpdatableReporter>();

    @Override
    public void error(String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.error(message);
        }
    }

    @Override
    public void warn(String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.warn(message);
        }
    }

    @Override
    public void info(String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.info(message);
        }
    }

    @Override
    public void start(String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.start(message);
        }
    }

    @Override
    public void complete(String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.complete(message);
        }
    }

    @Override
    public List<String> getErrors() {
    	if (reporters.isEmpty()) {
    		return Collections.<String>emptyList();
    	}
        return reporters.iterator().next().getErrors();
    }

    @Override
    public Reporter getResourceReporter(String path) {
        CompositeResourceReporter result = new CompositeResourceReporter(this);
        result.setPath(path);
        return result;
    }

    @Override
    public void setParameter(String name, Object value) {
        for (UpdatableReporter reporter : reporters) {
            reporter.setParameter(name, value);
        }
    }

    public void reportResourceError(String path, String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.getResourceReporter(path).error(message);
        }
    }

    // TODO why this method is public? 
    public void reportResourceWarn(String path, String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.getResourceReporter(path).warn(message);
        }
    }

    // TODO why this method is public?
    public void reportResourceInfo(String path, String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.getResourceReporter(path).info(message);
        }
    }

    // TODO why this method is public?
    public void reportResourceStart(String path, String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.getResourceReporter(path).start(message);
        }
    }

    // TODO why this method is public? 
    public void reportResourceComplete(String path, String message) {
        for (UpdatableReporter reporter : reporters) {
            reporter.getResourceReporter(path).complete(message);
        }
    }

    public void addReporter(UpdatableReporter reporter) {
        reporters.add(reporter);
    }

    private static class CompositeResourceReporter implements UpdatableReporter {

        private CompositeReporter parent;
        private String path;

        public void setPath(String path) {
            this.path = path;
        }

        public CompositeResourceReporter(CompositeReporter parent) {
            this.parent = parent;
        }

        @Override
        public void error(String message) {
            parent.reportResourceError(path, message);
        }

        @Override
        public void warn(String message) {
            parent.reportResourceWarn(path, message);
        }

        @Override
        public void info(String message) {
            parent.reportResourceInfo(path, message);
        }

        @Override
        public void start(String message) {
            parent.reportResourceStart(path, message);

        }

        @Override
        public void complete(String message) {
            parent.reportResourceComplete(path, message);
        }

        @Override
        public List<String> getErrors() {
            return parent.getErrors();
        }

        @Override
        public Reporter getResourceReporter(String path) {
            return this;
        }

        @Override
        public void setParameter(String name, Object value) {}

        @Override
        public List<String> getWarnings() {
            return parent.getWarnings();
        }

        @Override
        public void destroy() {
            // do nothing here
        }
    }

    @Override
    public List<String> getWarnings() {
        if (reporters.isEmpty()) {
            return Collections.<String>emptyList();
        }
        return reporters.iterator().next().getWarnings();
    }

    @Override
    public void destroy() {
        for (UpdatableReporter reporter : reporters) {
            reporter.destroy();
        }
    }
}
