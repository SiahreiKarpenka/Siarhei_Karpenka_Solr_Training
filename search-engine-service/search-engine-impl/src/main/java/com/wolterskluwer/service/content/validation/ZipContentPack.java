package com.wolterskluwer.service.content.validation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.wolterskluwer.csg.zipfileaccessor.Syntax;
import com.wolterskluwer.csg.zipfileaccessor.ZipFileAccessor;
import com.wolterskluwer.services.library.PackageNotFoundException;

public class ZipContentPack implements InputPack {

    /** Reference to the local ZIP file which represents the fetched pack. */
    private final File localFile;

    /** URL from which the pack is fetched */
    private final URL remoteUrl;

    /** Provides access to resources inside of a ZIP archive without unpacking */
    private ZipFileAccessor zipAccessor;

    /**
     * Constructs PCI content pack. It's important that both parameters should
     * be specified to create a <code>ContentPack</code> instance. The source
     * URL is then used to retrieve the real package name.
     * 
     * @param file
     * @param url
     */
    private ZipContentPack(File file, URL url) {
        localFile = file;
        remoteUrl = url;
    }

    /**
     * Retrieves the pack from the passed URL. The retrieved pack is saved as a
     * single ZIP file in the standard Java temp directory.
     * 
     * @param url
     * @return
     * @throws IOException
     */
    public static ZipContentPack fromURL(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("Pack URL is null.", new NullPointerException());
        }
        return new ZipContentPack(fetchFileFromUrl(url), url);
    }

    public static ZipContentPack fromURL(String str) throws IOException {
        URL url;
        try {
            url = URI.create(str).toURL();
        } catch (Exception e) {
            throw new PackageNotFoundException(str, e);
        }
        return fromURL(url);
    }

    /**
     * Copies data in the temp file and returns a reference to it.
     * 
     * @throws PackageNotFoundException
     */
    private static File fetchFileFromUrl(URL fromUrl) throws IOException {
        File toFile = TempDir.createUniqueFile(getBaseName(fromUrl), ".zip");
        FileOutputStream output = null;
        InputStream input = null;
        try {
            output = new FileOutputStream(toFile);
            input = fromUrl.openStream();
            IOUtils.copy(input, output);
        } catch(Exception e) {
            throw new PackageNotFoundException("Can't fetch file from url: " + fromUrl, e);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
        return toFile;
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        return getZipAccessor().getInputStream(path);
    }

    private ZipFileAccessor getZipAccessor() throws IOException {
        if (zipAccessor == null) {
            zipAccessor = new ValidationZipFileAccessor(new ZipFile(localFile));
        }
        return zipAccessor;
    }

    @Override
    public String getName() {
        return getBaseName(remoteUrl);
    }

    @Override
    public String getFileName() {
        return FilenameUtils.getName(remoteUrl.getPath());
    }

    public boolean delete() throws IOException {
        if (zipAccessor != null) {
            zipAccessor.close();
        }
        if (localFile != null) {
            return localFile.delete();
        }
        return true;
    }

    /**
     * Lists all paths relative to the base folder, with files that matches
     * wildcards
     * 
     * @param wildcardPattern
     *            a wildcard pattern to match file paths
     * @param caseSensitive 
     * @return a list of files that match the specified wildcard pattern
     */
    public List<String> listRelFilePaths(String wildcardPattern, boolean caseSensitive) throws IOException {
        // ZipFileAccessor.findZipEntries() doesn't recognize "**/*" glob
        // pattern; the trick below
        // fixes this (**.*)
        ArrayList<String> paths = new ArrayList<String>();
        ValidationZipFileAccessor accessor = (ValidationZipFileAccessor) getZipAccessor();
        accessor.setCaseSensitive(caseSensitive);
        List<ZipArchiveEntry> entries = accessor.findZipEntries(wildcardPattern, Syntax.GLOB); // **.*
        for (ZipEntry entry : entries) {
            paths.add(entry.getName());
        }
        return paths;
    }

    public String getSourceUrl() {
        return remoteUrl.toString();
    }

    /**
     * Retrieves the base of the file name from the given URL, that is the name
     * of the target file without extension.
     * 
     * @param url
     * @return
     */
    private static String getBaseName(URL url) {
        return FilenameUtils.getBaseName(url.getPath());
    }

    public File getLocalFile() {
        return localFile;
    }

    private static class TempDir {

        private static final String TMPDIR_PATH = System.getProperty("java.io.tmpdir");

        private static final File tmpDir = new File(TMPDIR_PATH);

        private static File createUniqueFile(String name, String ext) {
            String fileName = name;
            fileName += UUID.randomUUID().toString();
            fileName += ext;
            return new File(tmpDir, fileName);
        }
    }
}
