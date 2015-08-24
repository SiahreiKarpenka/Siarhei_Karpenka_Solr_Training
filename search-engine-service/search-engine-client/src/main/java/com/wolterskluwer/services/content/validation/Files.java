package com.wolterskluwer.services.content.validation;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

public class Files {

    private static final Logger log = Logger.getLogger(Files.class);

    public static File getResultFile(String filePath, String pathLabel, String defaultFilename)
            throws IOException {
        File result = null;
        if (filePath == null) {
            result = new File(defaultFilename);
            log.info(String
                    .format("Path for %s is not provided. Default file name will be used to save output in current folder: %s",
                            pathLabel, result.getAbsoluteFile()));
        } else {
            File file = new File(filePath);
            if (file.exists() && file.isDirectory()) {
                result = new File(file, defaultFilename);
                log.info(String
                        .format("Path for %s is a directory. Default file name will be used to save output: %s",
                                pathLabel, result.getAbsoluteFile()));
            } else {
                File directory = file.getParentFile();

                boolean isFile = new File(filePath).getName().contains(".");
                if (directory == null) {
                    if (isFile) {
                        result = file;
                        log.info(String.format(
                                "Path for %s is a relative filename. Using it to save output: %s",
                                pathLabel, result.getAbsoluteFile()));
                    } else {
                        result = new File(defaultFilename);
                        log.info(String
                                .format("Path for %s is folder which does not exist. Default file name will be used to save output in current folder: %s",
                                        pathLabel, result.getAbsoluteFile()));
                    }
                } else {
                    String filename = isFile ? new File(filePath).getName() : defaultFilename;
                    if (directory.exists() && isFile) {
                        result = new File(directory, filename);
                        log.info(String.format("Path for %s is valid. Result file is: %s",
                                pathLabel, result.getAbsoluteFile()));
                    } else {
                        result = new File(filename);
                        log.info(String
                                .format("Path for %s does not exist. Using current folder. Result file is: %s",
                                        pathLabel, result.getAbsoluteFile()));
                    }
                }
            }
        }
        if (result.exists() && result.isDirectory()) {
            String message = String
                    .format("We can't write file to path '%s' because folder with such name already exists. Remove the folder or change ouput path for %s.",
                            result.getAbsolutePath(), pathLabel);
            log.error(message);
            return null;
        }
        return result;
    }

    public static String fixRemoteFilename(String remoteFilename) {
        if (remoteFilename != null) {
            remoteFilename = remoteFilename.replace("\\", "/");
            return remoteFilename.contains("/") ? remoteFilename.substring(remoteFilename
                    .lastIndexOf("/") + 1) : remoteFilename;
        }
        return null;
    }

}
