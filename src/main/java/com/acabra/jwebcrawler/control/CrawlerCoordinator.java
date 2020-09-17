package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


    private final Logger logger = LoggerFactory.getLogger(CrawlerCoordinator.class);
    private static final Comparator<CrawledNode> PATH_COMPARATOR = (n1, n2) -> n2.url.compareTo(n1.url);
    private final Set<String> visited =  Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final ConcurrentHashMap<String, String> redirects = new ConcurrentHashMap<>();
    private final Set<String> failureLinks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final AtomicInteger ids = new AtomicInteger();
    private final Map<Long, Set<CrawledNode>> graph = new HashMap<>();
    private final ExecutorService ex;
    private LongAdder rejections = new LongAdder();
    private volatile boolean jobDone = false;

    CrawlerCoordinator(ExecutorService executorService) {
        this.ex = executorService;
    }

    /**
     * This method indicates whether or not the given link is allowed for processing, it should not have been
     * visited, not marked as failure and should start with the crawl site domain.
     * @param otherDomain
     * @return
     */
    public synchronized boolean allowLink(String otherDomain) {
        return !visited.contains(otherDomain) && !failureLinks.contains(otherDomain);
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

    public synchronized boolean reportFailureLink(String url) {
        return this.failureLinks.add(url);
    }

    public void reportRedirect(String url, String redirectUri) {
        this.redirects.putIfAbsent(url, redirectUri);
    }

    public String resolve(String link) {
        return this.redirects.getOrDefault(link, link);
    }

    public int getTotalRedirects() {
        return this.redirects.size();
    }

    public int getTotalFailures() {
        return this.failureLinks.size();
    }

    public void dispatchProducer(CrawlProducerWorker producer) {
        if (!jobDone) {
            try {
                CompletableFuture.runAsync(producer, ex);
            } catch (RejectedExecutionException ex) {
                rejections.increment();
            }
        } else {
            rejections.increment();
        }
    }

    public void requestJobDone() {
        this.jobDone = true;
    }

    public Long getTotalEnqueueRejections() {
        return rejections.sum();
    }

    public boolean isJobDone() {
        return jobDone;
    }
}
