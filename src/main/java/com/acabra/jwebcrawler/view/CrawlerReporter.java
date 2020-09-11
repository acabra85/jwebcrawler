package com.acabra.jwebcrawler.view;

import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlerReporter {

    private final static Logger logger = LoggerFactory.getLogger(CrawlerReporter.class);
    private static final PriorityQueue<CrawledNode> EMPTY_QUEUE = new PriorityQueue<>();
    final static String FILE_NAME_TEMPLATE = "CrawlReport-%s-%s.txt"; // e.g. CrawlReport-www.sitename.com-<identifier>.txt

    public CrawlerReporter() {
    }

    public static String buildReport(CrawlSiteResponse siteResponse) {

        Map<Long, PriorityQueue<CrawledNode>> graph = siteResponse.getGraph();
        Optional<CrawledNode> root = graph.getOrDefault(CrawledNode.ROOT_NODE_ID, EMPTY_QUEUE).stream().findFirst();
        if (root.isPresent()) {
            CrawledNode rootNode = root.get();
            Stack<CrawledNode> q = new Stack<>();
            q.add(rootNode);

            int totalLinks = graph.values().stream().mapToInt(Collection::size).sum();

            StringBuilder sb = new StringBuilder(String.format("\n\n---- Results for [%s] ----\n", rootNode.url))
                .append("\nTotal Pages crawled :").append(graph.keySet().size())
                .append("\nTotal Links Discovered: ").append(totalLinks)
                .append("\nTotal Links not downloadable due reporting failures: ").append(siteResponse.getTotalFailures())
                .append("\nTotal Links redirected: ").append(siteResponse.getTotalRedirects())
                .append(String.format("\nTotal time taken: %.3f seconds.\n", siteResponse.totalTime))
                .append("\n---------- Site Map ---------------\n");
            while (q.size()>0) {
                CrawledNode pop = q.pop();
                sb.append(String.format("%s%s\n", "---".repeat(pop.level), pop.url));
                PriorityQueue<CrawledNode> pq = graph.getOrDefault(pop.id, EMPTY_QUEUE);
                while(pq.size() > 0) {
                    q.push(pq.remove());
                }
            }
            sb.append("\n-----------------------------------\n");
            return sb.toString();
        }
        return "-- No Nodes Traversed --";
    }


    private static void writeReportToFile(String report, String siteURI) {
        try {
            String siteName = buildFileNameFromURI(siteURI);
            String filePath = String.format(FILE_NAME_TEMPLATE, siteName, System.currentTimeMillis());
            File resultsFolder = new File("reports");
            if (!resultsFolder.exists()) resultsFolder.mkdirs();
            Files.write(Paths.get("reports/"+filePath), report.getBytes());
        } catch (IOException e) {
            logger.error("Error while writing the report to file: " + e.getMessage());
        }
    }

    public static String buildFileNameFromURI(String fileName) {
        StringBuilder siteName = new StringBuilder();
        for (char c : fileName.split("//")[1].toCharArray()) {
            if ('.' == c || Character.isAlphabetic(c) || Character.isDigit(c)) {
                siteName.append(c);
            } else {
                siteName.append("_");
            }
        }
        return siteName.toString();
    }

    public static void report(boolean toFile, CrawlSiteResponse siteResponse, String siteURI) {
        String reportStr = buildReport(siteResponse);
        if (toFile) {
            writeReportToFile(reportStr, siteURI);
        } else {
            System.out.println(reportStr);
        }
    }
}
