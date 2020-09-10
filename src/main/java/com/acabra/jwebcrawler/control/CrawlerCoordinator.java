package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is thread safe, as all of its attributes are final and the public methods are either atomic
 * or synchronized.
 *
 * This object coordinates the work done by the workers, by controlling the links visited, the redirections needed
 * and the list of failures.
 *
 * This object stores the hierarchy representation of the crawled site.
 */
public class CrawlerCoordinator {

    private static final Comparator<CrawledNode> PATH_COMPARATOR = (n1, n2) -> n2.url.compareTo(n1.url);
    private final Set<String> visited =  Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final ConcurrentHashMap<String, String> redirects = new ConcurrentHashMap<>();
    private final Set<String> failureLinks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final AtomicInteger ids = new AtomicInteger();
    private final String base;
    private final Map<Long, Set<CrawledNode>> graph = new HashMap<>();
    private boolean jobDone = false;

    CrawlerCoordinator(String base) {
        this.base = base;
    }


    /**
     * This method indicates whether or not the given link is allowed for processing, it should not have been
     * visited, not marked as failure and should start with the crawl site domain.
     * @param otherDomain
     * @return
     */
    public synchronized boolean allowLink(String otherDomain) {
        return otherDomain.startsWith(this.base)
                && !visited.contains(otherDomain)
                && !failureLinks.contains(otherDomain);
    }

    public int getNextId() {
        return this.ids.getAndIncrement();
    }

    public synchronized void processNode(CrawledNode node) {
        this.visited.add(node.url);
        this.graph.computeIfAbsent(node.parentId, newKey -> new HashSet<>()).add(node);
    }

    /**
     * Returns a copy of the crawled site graph for one where the child nodes of every
     * parent page are sorted in descending order, this allows the DFS traversal to print
     * the site map in ascending order for sibling pages at each level.
     * @return
     */
    public synchronized Map<Long, PriorityQueue<CrawledNode>> getGraph() {
        Map<Long, PriorityQueue<CrawledNode>> map = new HashMap<>();
        this.graph.forEach((key, value) -> {
            PriorityQueue<CrawledNode> nodes = new PriorityQueue<>(PATH_COMPARATOR);
            nodes.addAll(value);
            map.put(key, nodes);
        });
        return Collections.unmodifiableMap(map);
    }

    public synchronized void reportFailureLink(String url) {
        this.failureLinks.add(url);
    }

    public void reportRedirect(String url, String redirectUri) {
        this.redirects.putIfAbsent(url, redirectUri);
    }

    public String resolve(String link) {
        return this.redirects.getOrDefault(link, link);
    }

    public synchronized boolean isJobDone() {
        return jobDone;
    }

    public synchronized void requestJobDone() {
        this.jobDone = true;
    }

    public int getTotalRedirects() {
        return this.redirects.size();
    }

    public int getTotalFailures() {
        return this.failureLinks.size();
    }
}