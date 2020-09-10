package com.acabra.jwebcrawler.model;


import java.util.Map;
import java.util.PriorityQueue;

public class CrawlSiteResponse {
    final Map<Long, PriorityQueue<CrawledNode>> graph;
    final int totalRedirects;
    final int totalFailures;
    final public double totalTime;

    public CrawlSiteResponse(Map<Long, PriorityQueue<CrawledNode>> graph, int totalRedirects, int totalFailures,
                             double totalTime) {
        this.graph = graph;
        this.totalRedirects = totalRedirects;
        this.totalFailures = totalFailures;
        this.totalTime = totalTime;
    }

    public Map<Long, PriorityQueue<CrawledNode>> getGraph() {
        return graph;
    }

    public int getTotalRedirects() {
        return totalRedirects;
    }

    public int getTotalFailures() {
        return totalFailures;
    }

    public double getTotalTime() {
        return totalTime;
    }
}
