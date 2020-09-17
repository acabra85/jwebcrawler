package com.acabra.jwebcrawler.view;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.*;

public class FileWriterAppender implements Appender, AutoCloseable {

    private static final String FILE_NAME_TEMPLATE = "CrawlReport-%s-%s.txt"; // e.g. CrawlReport-www.sitename.com-<identifier>.txt
    private static final String SEPARATOR = System.getProperty("file.separator");
    private static final String REPORT_DEST_FOLDER = "reports";

    private final String fileName;
    private BufferedWriter bw;

    public FileWriterAppender(String siteURI, String identifier) throws IOException {
        this.fileName = buildFileName(siteURI, identifier);
        String filePath = String.join(SEPARATOR, REPORT_DEST_FOLDER, fileName);
        this.bw = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8));
    }

    @Override
    public Appender append(String text) throws IOException {
        bw.write(text);
        return this;
    }

    @Override
    public void close() throws IOException {
        bw.close();
    }

    @Override
    public Optional<String> textIfAvailable() {
        return Optional.empty();
    }

    @SuppressFBWarnings(value ="RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
            justification="checking whether folder exists or not, not necessary to store return")
    private static String buildFileName(String domain, String identifier) {
        String siteName = buildFileNameFromURI(domain.contains("//") ? domain.split("//")[1] : domain);
        String filePath = String.format(FILE_NAME_TEMPLATE, siteName, identifier);
        File resultsFolder = new File(REPORT_DEST_FOLDER);
        if (!resultsFolder.exists()) resultsFolder.mkdirs();
        return filePath;
    }


    public static String buildFileNameFromURI(String fileName) {
        StringBuilder siteName = new StringBuilder();
        for (char c : fileName.toCharArray()) {
            if ('.' == c || '-' == c || '_' == c || Character.isAlphabetic(c) || Character.isDigit(c)) {
                siteName.append(c);
            } else {
                siteName.append("_");
            }
        }
        return siteName.toString();
    }

    public String getFileName() {
        return fileName;
    }
}
