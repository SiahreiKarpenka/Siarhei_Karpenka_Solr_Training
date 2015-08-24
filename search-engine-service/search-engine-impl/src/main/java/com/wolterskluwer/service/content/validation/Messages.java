package com.wolterskluwer.service.content.validation;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wolterskluwer.services.library.xml.fileUtils.LocalResolver;

public class Messages {

    public static class PropertyName {
        //jvm property to locate config file
        public static final String MESSAGES_PATH = "com.wolterskluwer.service.content.validation.messages_path";
        //standard name of the config file (should be in classpath)
        private static final String DEFAULT_MESSAGES_PATH = "messages.properties";
    }

    private static volatile Messages instance;
    private static final Logger log = LoggerFactory.getLogger(Messages.class);
    
    public static Messages getInstance() {
        Messages tmpInstance = instance;
        if (tmpInstance == null) {
            synchronized (Messages.class) {
                tmpInstance = instance;
                if (tmpInstance == null) {
                    instance = tmpInstance = new Messages();
                }
            }
        }
        return tmpInstance;
    }

    private final Properties messages = new Properties();

    private Messages() {
        loadProperties();
    }

    private void loadProperties() {
        String fileName = PropertyName.DEFAULT_MESSAGES_PATH;
        if (System.getProperty(PropertyName.MESSAGES_PATH) != null)
            fileName = System.getProperty(PropertyName.MESSAGES_PATH);
        try {
            messages.load(LocalResolver.getInputStream(fileName));
        } catch (IOException ex) {
            log.warn("Can't load external properties file " + fileName, ex);
        }
    }


    public String getMessage(String key) {
        return messages.getProperty(key);
    }
}
