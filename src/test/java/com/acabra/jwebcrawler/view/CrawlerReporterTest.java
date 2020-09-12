package com.acabra.jwebcrawler.view;

import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CrawlerReporterTest {


    private static final String SUB_DOMAIN = "example.com";
    private static final Comparator<CrawledNode> COMPARATOR = (a,b) -> a.url.compareTo(b.url);

    @Test
    public void should_build_empty_report() {
        String actualReport = CrawlerReporter.buildReport(
                new CrawlSiteResponse(SUB_DOMAIN, Collections.emptyMap(), 0, 0, 0.5));
        Assertions.assertEquals(actualReport, CrawlerReporter.EMPTY_REPORT_CONTENT);
    }

    @Test
    public void should_build_non_empty_report() {
        Map<Long, PriorityQueue<CrawledNode>> graph = buildGraph();
        String actualReport = CrawlerReporter.buildReport(new CrawlSiteResponse(SUB_DOMAIN, graph, 0, 0, 0.5));
        Assertions.assertTrue(actualReport.contains(CrawlerReporter.SITE_MAP_HEADER));
        Assertions.assertTrue(actualReport.contains(SUB_DOMAIN));
        Assertions.assertTrue(actualReport.contains("page.html"));
    }

    private Map<Long, PriorityQueue<CrawledNode>> buildGraph() {
        Map<Long, PriorityQueue<CrawledNode>> graph = new HashMap<>();
        CrawledNode rootNode = new CrawledNode(SUB_DOMAIN, 0L);
        CrawledNode childRoot = new CrawledNode(SUB_DOMAIN + "/page.html", 1L, rootNode.level + 1, rootNode.id);
        graph.put(CrawledNode.ROOT_NODE_PARENT_ID, new PriorityQueue<>(COMPARATOR){{
            offer(rootNode);
        }});
        graph.put(rootNode.id, new PriorityQueue<>(COMPARATOR){{
            offer(childRoot);
        }});
        return graph;
    }

}