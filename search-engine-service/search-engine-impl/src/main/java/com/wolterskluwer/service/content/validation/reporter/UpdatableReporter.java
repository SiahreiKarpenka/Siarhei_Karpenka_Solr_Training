package com.wolterskluwer.service.content.validation.reporter;

public interface UpdatableReporter extends Reporter {
    public void setParameter(String name, Object value);
}
