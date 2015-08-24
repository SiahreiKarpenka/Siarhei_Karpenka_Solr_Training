package com.wolterskluwer.service.content.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import com.wolterskluwer.csg.zipfileaccessor.Syntax;
import com.wolterskluwer.csg.zipfileaccessor.ZipFileAccessor;
import com.wolterskluwer.csg.zipfileaccessor.ZipUtils;

/**
 * This class was created to fix issue with "**\/*" pattern in ZipFileAccessor
 * class.
 * 
 * */
// TODO remove this class when ZipFileAccessor will be fixed
public class ValidationZipFileAccessor extends ZipFileAccessor {

    boolean caseSensitive = true;
    
    public ValidationZipFileAccessor(ZipFile zipFile) {
        super(zipFile);
    }

    @Override
    public List<ZipArchiveEntry> findZipEntries(String pattern, Syntax syntax) {
        List<ZipArchiveEntry> result = new ArrayList<ZipArchiveEntry>();

        String regexpPattern = pattern;

        if (syntax == Syntax.GLOB) {
            regexpPattern = ZipUtils.toRegexPattern(pattern);
        }
        Pattern ptrn = Pattern.compile(regexpPattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);

        for (ZipArchiveEntry entry : getZipEntries()) {
            String entryName = entry.getName().replace("\\", "/");
            // added workaround
            boolean fileInRootFolder = !entryName.contains("/");
            boolean allLevelsPattern = pattern.startsWith("**");
            if (ptrn.matcher(entryName).matches()
                    || (fileInRootFolder && allLevelsPattern && ptrn.matcher("/" + entryName)
                            .matches())) {
                result.add(entry);
            }
        }

        return result;
    }
    
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
}
