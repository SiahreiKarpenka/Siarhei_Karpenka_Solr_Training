package com.wolterskluwer.service.content.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Describes a pack sent to validation.
 */
public interface InputPack {

    /**
     * Gets an <code>InputStream</code> instance for the resource in the pack pointed by the
     * given relative path.
     *
     * @param path must not be <code>null</code>; a relative path pointing to a resource within the
     *             pack
     *
     * @return an <code>InputStream</code> instance of the resource pointed by the given relative
     * path
     */
    InputStream getInputStream(String path) throws IOException;

    /**
     * Retrieves the name of the package which is identical to the pack's file name without
     * the extension.
     *
     * @return the name of the pack
     */
    String getName();

    /**
     * Retrieve the pack's file name including the extension.
     * @return the pack file name (including extension)
     */
    String getFileName();
    
    /**
     * Deletes package from the disk.
     * */
    boolean delete() throws IOException;
    
    /**
     * Lists files in package.
     * */
    public List<String> listRelFilePaths(String wildcardPattern, boolean caseSensitive) throws IOException;
}