package com.wolterskluwer.service.content.validation.orchestration;

import de.odysseus.el.util.SimpleContext;
import de.odysseus.el.util.SimpleResolver;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

/**
 * Keeps properties that can be resolved in expression.
 */
public class PropertyContext {

    private static ExpressionFactory factory = ExpressionFactory.newInstance();

    private final SimpleContext context;

    public PropertyContext() {
        context = new SimpleContext(new SimpleResolver());
    }

    public void declareProperty(String name, String value) {
        ValueExpression stringExpression = factory.createValueExpression(value, String.class);
        context.setVariable(name, stringExpression);
    }

    public String resolve(String exp) {
        ValueExpression value = factory.createValueExpression(context, exp, String.class);
        return (String) value.getValue(context);
    }
}
