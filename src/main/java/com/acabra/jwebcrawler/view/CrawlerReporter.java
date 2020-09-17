package com.acabra.jwebcrawler.view;

import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlerReporter {


    private static final Logger logger = LoggerFactory.getLogger(CrawlerReporter.class);
    private static final PriorityQueue<CrawledNode> EMPTY_QUEUE = new PriorityQueue<>();
    private static final String RESULTS_HEADER_TEMPLATE = "%n%n---- Results for [%s] ----%n";
    static final String EMPTY_REPORT_CONTENT = "-- No Nodes Traversed --";
    static final String SITE_MAP_HEADER = "\n---------- Site Map ---------------\n";

    public CrawlerReporter() {
    }

    Optional<String> buildReport(CrawlSiteResponse siteResponse, Appender appender) {
        try {
            Map<Long, PriorityQueue<CrawledNode>> graph = siteResponse.getGraph();
            Optional<CrawledNode> root = graph.getOrDefault(CrawledNode.ROOT_NODE_PARENT_ID, EMPTY_QUEUE).stream().findFirst();
            if (root.isPresent()) {
                appender.append(String.format(RESULTS_HEADER_TEMPLATE, siteResponse.getSiteURI()));
                CrawledNode rootNode = root.get();
                Stack<CrawledNode> q = new Stack<>();
                q.add(rootNode);

                int totalLinks = graph.values().stream().mapToInt(Collection::size).sum();

                appender
                        .append("\nTotal Concurrent Workers: ").append("" + siteResponse.getWorkerCount())
                        .append("\nTotal Pages crawled: ").append("" + graph.keySet().size())
                        .append("\nTotal Links Discovered: ").append("" + totalLinks)
                        .append("\nTotal Links not downloadable due reporting failures: ").append("" + siteResponse.getTotalFailures())
                        .append("\nTotal Links rejected after timeout: ").append("" + siteResponse.getRejections())
                        .append("\nTotal Links redirected: ").append("" + siteResponse.getTotalRedirects())
                        .append(String.format("%nTotal time taken: %.3f seconds.%n", siteResponse.getTotalTime()))
                        .append(SITE_MAP_HEADER);
                while (q.size() > 0) {
                    CrawledNode pop = q.pop();
                    appender.append(String.format("%s%s%n", "---".repeat(pop.level), pop.url));
                    PriorityQueue<CrawledNode> pq = graph.getOrDefault(pop.id, EMPTY_QUEUE);
                    while (pq.size() > 0) {
                        q.push(pq.remove());
                    }
                }
                return appender
                        .append("\n-----------------------------------\n")
                        .textIfAvailable();
            }
            return appender
                    .append(EMPTY_REPORT_CONTENT)
                    .textIfAvailable();
        } catch (IOException e) {
            String errorText = "Unable to generate response for site: " + siteResponse.getSiteURI();
            logger.error(errorText);
            return Optional.of(errorText);
        } finally {
            try {
                appender.close();
            } catch (IOException ioe) {
                logger.error(ioe.getMessage(), ioe);
            }
        }
    }

    public void report(boolean toFile, CrawlSiteResponse siteResponse, String identifier) {
        try {
            Appender appender;
            if (toFile) {
                appender = new FileWriterAppender(siteResponse.getSiteURI(), identifier);
            } else {
                appender = new ConsoleAppender();
            }
            buildReport(siteResponse, appender);
        } catch (Exception io) {
            logger.error(io.getMessage(), io);
        }
    }

}
