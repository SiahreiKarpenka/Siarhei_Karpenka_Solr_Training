package com.wolterskluwer.service.content.validation.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wolterskluwer.service.content.validation.OrchestrationExecutor;
import com.wolterskluwer.service.content.validation.ValidationException;
import com.wolterskluwer.service.content.validation.ValidationServiceConfiguration;
import com.wolterskluwer.service.content.validation.ZipContentPack;
import com.wolterskluwer.service.mime.MimeType;
import com.wolterskluwer.service.mime.MimeTypeUtil;
import com.wolterskluwer.service.util.ContentEncoder;

public class OrchestrationContextFactory {

    private static final String PACKAGES_PROPERTIES_LOCATION = "/packages.properties";
    
    private static final Logger logger = LoggerFactory.getLogger(OrchestrationExecutor.class);

    private static final List<String> packageBaseMimeTypes = new ArrayList<String>();

    static {
        initializePackageBaseMimeTypes();
    }

    private static void initializePackageBaseMimeTypes() {
        try {
            Properties properties = new Properties();
            properties.load(OrchestrationContextFactory.class.getResourceAsStream(PACKAGES_PROPERTIES_LOCATION));
            for (String propertyName : properties.stringPropertyNames()) {
                packageBaseMimeTypes.add(propertyName);
            }
        } catch (Exception e) {
            logger.error("Can't load packages mimetypes list from " + PACKAGES_PROPERTIES_LOCATION, e);
        }
    }

    public static OrchestrationContext createContext(
            ValidationServiceConfiguration configuration,
            String contentObjectId,
            String data,
            MimeType mimeType) throws IOException, Base64DecodingException, ValidationException {

        if (isPackage(mimeType)) {
            ZipContentPack contentPack;
            if (!MimeTypeUtil.isUrl(mimeType)) {
                File toFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
                byte[] decoded = ContentEncoder.decode(data, mimeType);
                FileUtils.writeByteArrayToFile(toFile, decoded);
                contentPack = ZipContentPack.fromURL("file:///" + toFile.getAbsolutePath().replace(File.separator, "/"));
                toFile.delete();
            } else {
                contentPack = ZipContentPack.fromURL(data);
            }
            return new ZipPackageOrchestrationContext(configuration, contentObjectId, contentPack);
        } else {
            File file = File.createTempFile("vtmp", null);
            FileOutputStream out = new FileOutputStream(file);
            try {
                IOUtils.write(ContentEncoder.decode(data, mimeType), out);
            } finally {
                IOUtils.closeQuietly(out);
            }
            return new FileOrchestrationContext(configuration, file, contentObjectId);
        }
    }

    private static boolean isPackage(MimeType mimeType) {
        return packageBaseMimeTypes.contains(mimeType.getBase()) || MimeTypeUtil.isUrl(mimeType);
    }
}
