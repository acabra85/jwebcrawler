package com.acabra.jwebcrawler.view;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWriterAppender implements Appender {

    private static final String FILE_NAME_TEMPLATE = "CrawlReport-%s-%s.txt"; // e.g. CrawlReport-www.sitename.com-<identifier>.txt
    private static final Logger logger = LoggerFactory.getLogger(FileWriterAppender.class);
    private static final String SEPARATOR = System.getProperty("file.separator");
    private static final String REPORT_DEST_FOLDER = "reports";

    private final String fileName;
    private BufferedWriter bw;

    public FileWriterAppender(String siteURI, String identifier) throws IOException {
        this.fileName = buildFileName(siteURI, identifier);
        String filePath = String.join(SEPARATOR, REPORT_DEST_FOLDER, fileName);
        this.bw = new BufferedWriter(new FileWriter(filePath));
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
