package com.acabra.jwebcrawler.view;

import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;

import java.util.*;

public class CrawlerReporter {

    private static final PriorityQueue<CrawledNode> EMPTY_QUEUE = new PriorityQueue<>();

    public CrawlerReporter() {
    }

    public static String buildReport(CrawlSiteResponse siteResponse) {

        Map<Long, PriorityQueue<CrawledNode>> graph = siteResponse.getGraph();
        Optional<CrawledNode> root = graph.getOrDefault(-1L, EMPTY_QUEUE).stream().findFirst();
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
}
