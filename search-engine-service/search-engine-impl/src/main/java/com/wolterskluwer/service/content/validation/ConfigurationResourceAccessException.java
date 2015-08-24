package com.wolterskluwer.service.content.validation;


/**
 * Signals that an attempt access a specified configuration resource has failed.
 */
public class ConfigurationResourceAccessException extends ValidationException {

    /**
     * 
     */
    private static final long serialVersionUID = -1419593579789500468L;

    /**
     * Constructs new <code>ConfigurationResourceAccessException</code> instance
     * with the given detail message.
     *
     * @param message the detail message
     */
    public ConfigurationResourceAccessException(String message) {
        super(message);
    }
}
