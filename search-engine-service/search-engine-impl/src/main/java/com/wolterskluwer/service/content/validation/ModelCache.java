package com.wolterskluwer.service.content.validation;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class ModelCache {

    private static Logger log = LoggerFactory.getLogger(ModelCache.class);

    private static FileManager fileManager = FileManager.get();

    private final ConcurrentHashMap<String, Model> concurrentCache
            = new ConcurrentHashMap<String, Model>();

    /**
     * Thread-safe method for loading RDF models.
     * @param filenameOrURI
     * @return an RDF model
     * @throws ValidationException if the operation failed
     */
    public Model getModel(String filenameOrURI) throws ValidationException {
        // TODO keep weak references
        Model model = concurrentCache.get(filenameOrURI);
        if (model == null) {
            synchronized (this) {
                model = concurrentCache.get(filenameOrURI);
                if (model == null) {
                    log.debug("Loading model from {}", filenameOrURI);
                    model = loadModel(filenameOrURI);
                    concurrentCache.put(filenameOrURI, model);
                }
            }
        }
        return model;
    }

    private Model loadModel(String filenameOrURI) throws ValidationException {
        try {
            return fileManager.loadModel(filenameOrURI); // Sometimes Null Pointer can occur in case of wrong model
        } catch (Exception ex) {
            throw new ValidationException(String.format(Messages.getInstance().getMessage("msg.rdf.readModelException"), filenameOrURI), ex);
        }
    }
}