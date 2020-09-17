package com.acabra.jwebcrawler.model;


import java.util.Map;
import java.util.PriorityQueue;

public class CrawlSiteResponse {
    final Map<Long, PriorityQueue<CrawledNode>> graph;
    final int totalRedirects;
    final int totalFailures;
    private final double totalTime;
    private final String siteURI;
    private final Long rejections;
    private final int workerCount;


    public CrawlSiteResponse(String siteURI, Map<Long, PriorityQueue<CrawledNode>> graph, int totalRedirects,
                             int totalFailures, Long rejections, double totalTime, int workerCount) {
        this.siteURI = siteURI;
        this.graph = graph;
        this.totalRedirects = totalRedirects;
        this.totalFailures = totalFailures;
        this.rejections = rejections;
        this.totalTime = totalTime;
        this.workerCount = workerCount;
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

    public String getSiteURI() {
        return siteURI;
    }

    public long getRejections() {
        return rejections;
    }

    public int getWorkerCount() {
        return workerCount;
    }
}
