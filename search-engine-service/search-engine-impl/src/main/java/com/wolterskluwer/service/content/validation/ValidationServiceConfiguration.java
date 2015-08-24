package com.wolterskluwer.service.content.validation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import com.wolterskluwer.service.discovery.api.PlanArtifact;
import com.wolterskluwer.service.util.FileUtil;

public class ValidationServiceConfiguration {

    private final PlanArtifact plan;

    //TODO: update modelCache if configuration was updated.
    private final ModelCache modelCache = new ModelCache();

    public ValidationServiceConfiguration(PlanArtifact plan) {
        if (plan == null) {
            throw new IllegalArgumentException(new NullPointerException());
        }
        this.plan = plan;
    }

    public File getFileResource(String path)
            throws ConfigurationResourceAccessException {
        try {
			return new File(plan.getBaseDirectory(), path);
        } catch (Exception ex) {
            throw new ConfigurationResourceAccessException(
                    "Configuration resource is not accessible: " +
                            path);
        }
    }

    public Model getModel(String filenameOrUri) {
        return FileManager.get().loadModel(filenameOrUri);
    }

    /**
     * Retrieves an RDF model by the given file path or URI. It uses caching
     * mechanism internally so that the same model is not loaded twice.
     *
     * Recommended to use this method only for loading large RDF models in order
     * to keep the cache clean.
     *
     * @param filenameOrUri a file path or URI to get a model
     * @return an RDF model
     * @throws ValidationException
     */
    public Model getCacheModel(String filenameOrUri) throws ValidationException {
        return modelCache.getModel(filenameOrUri);
    }

    private List<File> fileResources(String wildcardPattern) {
        return FileUtil.listDirectoryFiles(plan.getBaseDirectory(), wildcardPattern);
    }

    public List<File> listFileResources(String wildcardPattern)
            throws ConfigurationResourceAccessException {
        List<File> files = fileResources(wildcardPattern);
        if (files.isEmpty()) {
            throw new ConfigurationResourceAccessException(
                    "No configuration resources matched by the pattern: " +
                            wildcardPattern);
        }
        return files;
    }

    /**
     * Get list of Files by patterns from configuration
     * Returns only unique Files. 
     * @param wildcardPatterns - patterns like path/folder/*.txt
     * @return array of files that were found.
     */
    public List<File> listFilesByPattern(String[] wildcardPatterns)
            throws ConfigurationResourceAccessException {
        Set<File> files = new HashSet<File>();
        for (String wildcardPattern : wildcardPatterns) {
            files.addAll(fileResources(wildcardPattern));
        }
        if (files.isEmpty()) {
            throw new ConfigurationResourceAccessException(
                    "No configuration resources matched by the patterns: " +
                            join(wildcardPatterns, ","));
        }
        return new ArrayList<File>(files);
    }

    private static String join(String[] arr, String ch) {
        if (arr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(arr[0]);
        for (int i = 1; i < arr.length; i++) {
            sb.append(ch);
            sb.append(arr[i]);
        }
        return sb.toString();
    }

//    public String getLocation() {
//        return configuration.getLocation();
//    }
//
    public PlanArtifact getPlan() {
        return plan;
    }

}

