package com.wolterskluwer.service.content.validation.validator;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.FileManager;
import com.wolterskluwer.service.content.validation.ValidationException;

public class ValidationContext {

    private static FileManager fileManager = FileManager.get();

    private final LModelCache modelCache = new LModelCache();

    /**
     * Retrieves an RDF model from the given file. This method caches models for subsequent usage
     * so should be used carefully, it will always return the same model for a file even if
     * the content of is is changed.
     *
     * @param file a file to retrieve a model from
     * @return a model loaded from a file
     */
    public Model getModel(File file) throws ValidationException {
        Model model = modelCache.getModel(file);
        if (model == null) {
            try {
            	model = loadModelFromFile(file);
        	} catch (JenaException e) {
        		throw new ValidationException("Error while reading model: " + StringEscapeUtils.escapeXml(e.getMessage()), e);
        	}
            if (model != null) {
                modelCache.putModel(file, model);
            }
        }
        return model;
    }
    
    /**
     * Retrieves an RDF model from the given InputStream. This method caches models for subsequent usage
     * so should be used carefully.
     *
     * @param inputStream an InputStream instance
     * @param path to file uses as a cache key
     * @return a model loaded from a file
     */
    public Model getModel(InputStream inputStream, String path) throws ValidationException {
    	try {
    		return getModelFromInputStream(inputStream);
    	} catch (JenaException e) {
    		throw new ValidationException("Error while reading model: " + StringEscapeUtils.escapeXml(e.getMessage()), e);
    	}
        // TODO caching functionality needs to be fixed
//        Model model = modelCache.getModel(path);
//        if (model == null) {
//            model = getModelFromInputStream(inputStream);
//            if (model != null) {
//                modelCache.putModel(path, model);
//            }
//        }
//        return model;
    }

    /**
     * @param file a file to load a model
     * @return a model loaded from the given file
     * @throws com.hp.hpl.jena.shared.JenaException in case of a syntax error in the file
     */
    private Model loadModelFromFile(File file) {
        return fileManager.loadModel(file.getAbsolutePath());
    }

    /**
     * 
     * @param inputSteram - inputStream that contains RDf file
     * @return RDF Model
     */
    private Model getModelFromInputStream(InputStream inputSteram) {
        try {
            Model model = ModelFactory.createDefaultModel();
            return model.read(inputSteram, null);
        } finally {
            IOUtils.closeQuietly(inputSteram);
        }
    }

    /**
     * Simple model cache. This class is not thread-safe and should be used only inside of one
     * ValidationContext.
     */
    private static class LModelCache {
        private final HashMap<String, Model> models = new HashMap<String, Model>();

        private static String key(File file) {
            return file.getAbsolutePath();
        }

        @Deprecated
        public Model getModel(File file) {
            String key = key(file);
            return models.get(key);
        }

        public Model getModel(String path) {
            return models.get(path);
        }

        @Deprecated
        public void putModel(File file, Model model) {
            String key = key(file);
            models.put(key, model);
        }

        public void putModel(String path, Model model) {
            models.put(path, model);
        }
    }
}

