package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.service.DownloadService;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represent the individual worker with the task of downloading the content of the given uri
 */
public class CrawlWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CrawlWorker.class);
    private static final int MAX_BUDGET = 8;
    private static final int EARNED_BUDGET = 2;
    private static final long MAX_CHILD_PER_PAGE = 10;
    private static final int MAX_DEPTH = 10;

    private final BlockingQueue<CrawledNode> queue;
    private final CrawlerCoordinator coordinator;
    private final ReentrantLock queueLock;
    private final boolean stoppable;
    private final int maxSiteHeight;
    private int workBudget; //budget units when reaches zero allows graceful termination

    private final List<String> EMPTY_RESPONSE = Collections.emptyList();
    private final String siteURI;
    private final long sleepTime;
    private DownloadService downloadService;
    private long maxChildPerPage;

    CrawlWorker(BlockingQueue<CrawledNode> blockingQ, CrawlerCoordinator coordinator, String siteURI,
                long sleepTime, ReentrantLock queueLock, boolean stoppable, int maxChildPerPage,
                int maxSiteHeight, DownloadService downloadService) {
        this.queue = blockingQ;
        this.coordinator = coordinator;
        this.workBudget = MAX_BUDGET;
        this.siteURI = siteURI;
        this.sleepTime = sleepTime;
        this.queueLock = queueLock;
        this.stoppable = stoppable;
        this.maxChildPerPage = Math.min(MAX_CHILD_PER_PAGE, maxChildPerPage);
        this.maxSiteHeight = Math.min(MAX_DEPTH, maxSiteHeight);
        this.downloadService = downloadService;
    }

    public static CrawlWorker of(BlockingQueue<CrawledNode> queue, CrawlerCoordinator coordinator,
                                 String siteURI, long sleepTime, ReentrantLock queueLock, boolean stoppable,
                                 int maxChildPerPage, int maxSiteHeight, DownloadService downloadService) {
        return new CrawlWorker(queue, coordinator, siteURI, sleepTime, queueLock, stoppable, maxChildPerPage,
                maxSiteHeight, downloadService);
    }

    static class ProcessedResponse {
        final List<String> links;
        final int statusCode;
        final public boolean success;

        ProcessedResponse(int statusCode, List<String> links, boolean success) {
            this.statusCode = statusCode;
            this.links = links;
            this.success = success;
        }

        static ProcessedResponse ofError(String url) {
            return new ProcessedResponse(0, List.of(url), false);
        }
    }

    private ProcessedResponse processHTTPResponse(CrawledNode node, String siteUri, HttpResponse<String> httpResponse) {
        String uri = node.url;
        coordinator.processNode(node);
        if (null == httpResponse) {
            logger.info("Error while retrieving content from url: " + node.url);
            return ProcessedResponse.ofError(node.url);
        }
        if (isResponseHtml(httpResponse.headers())) {
            int statusCode = httpResponse.statusCode();
            List<String> links = EMPTY_RESPONSE;
            if (statusCode == 200) {
                links = extractLinks(String.valueOf(httpResponse.body()), siteUri);
            } else if (statusCode == 301 || statusCode == 302) {
                List<String> location = httpResponse.headers().map().get("Location");
                if (location!= null && location.size() > 0) {
                    links = location;
                    logger.info(String.format("[%s] permanently moved to [%s] call code: %d", uri, links.get(0), statusCode));
                }
            } else {
                coordinator.reportFailureLink(uri);
                logger.info(String.format("Failed GET uri: [%s] call code: %d", uri, statusCode));
            }
            return new ProcessedResponse(statusCode, links, true);
        } else {
            coordinator.reportFailureLink(uri);
            logger.info(String.format("Content type is not text/html {%s}", uri));
            return ProcessedResponse.ofError(node.url);
        }
    }

    private boolean isResponseHtml(HttpHeaders headers) {
        Optional<String> contentTypeHtml = headers.allValues("content-type").stream().filter(value -> value.contains("text/html")).findFirst();
        return contentTypeHtml.isPresent();
    }

    List<String> extractLinks(String htmlResponseBody, String baseUri) {
        Elements links = Jsoup.parse(htmlResponseBody, baseUri).select("a");
        return links.stream()
                .map(element -> element.attr("abs:href"))
                .filter(link -> link.startsWith(this.siteURI))
                .limit(this.maxChildPerPage)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void run() {
        if(stoppable) {
            runStoppable();
        } else {
            try {
                while (true) {
                    if (queue.size() > 0 && this.queueLock.tryLock()) {
                        final CrawledNode node = queue.take();
                        this.queueLock.unlock();
                        String resolve = coordinator.resolve(node.url);
                        if (coordinator.allowLink(resolve)) {
                            CountDownLatch latch = new CountDownLatch(1);
                            downloadService.download(resolve)
                                    .thenApply(resp -> processHTTPResponse(node, this.siteURI, resp))
                                    .thenAccept(pResponse -> enqueueLocalDomainLinksFound(node, pResponse))
                                    .handle((res,err) -> {
                                        latch.countDown();
                                        taskCompletedAwardBudget();
                                        return res;
                                    });
                            latch.await();
                        }
                    } else if (this.workBudget > 0) {
                        --this.workBudget;
                        Thread.sleep(sleepTime);
                    }else { return; }
                }
            } catch (InterruptedException ie) {
                logger.error("CrawlWorker interrupted: " + ie.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                if (this.queueLock.isHeldByCurrentThread()) {
                    this.queueLock.unlock();
                }
                logger.info("worker finished");
            }
        }
    }

    private void runStoppable() {
        try {
            while (true) {
                if (coordinator.isJobDone()) {
                    logger.info("Coordinator requested job completion");
                    return;
                } else if (queue.size() > 0 && this.queueLock.tryLock()) {
                    final CrawledNode node = queue.take();
                    this.queueLock.unlock();
                    String resolve = coordinator.resolve(node.url);
                    if (coordinator.allowLink(resolve)) {
                        CountDownLatch latch = new CountDownLatch(1);
                        this.downloadService.download(resolve)
                                .thenApply(resp -> processHTTPResponse(node, this.siteURI, resp))
                                .thenAccept(pResponse -> enqueueLocalDomainLinksFound(node, pResponse))
                                .handle((res,err) -> {
                                    latch.countDown();
                                    taskCompletedAwardBudget();
                                    return res;
                                });
                        latch.await();
                    }
                } else if (this.workBudget > 0) {
                    --this.workBudget;
                    Thread.sleep(sleepTime);
                } else { return; }
            }
        } catch (InterruptedException ie) {
            logger.error("CrawlWorker interrupted: " + ie.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            if (this.queueLock.isHeldByCurrentThread()) {
                this.queueLock.unlock();
            }
            logger.info("worker finished");
        }
    }

    private void enqueueLocalDomainLinksFound(CrawledNode node, ProcessedResponse pResponse) {
        if (pResponse.success) {
            if (pResponse.statusCode == 200) {
                pResponse.links.forEach(link -> {
                    if (allowEnqueue(node, link)) {
                        queue.offer(new CrawledNode(link, coordinator.getNextId(), node.level + 1, node.id));
                    }
                });
            } else if (pResponse.statusCode == 301 || pResponse.statusCode == 302) {
                // requeue the page with the given redirection link
                if (pResponse.links != null && pResponse.links.size() > 0) {
                    String newLocation = pResponse.links.get(0);
                    String redirectUri = newLocation.startsWith(this.siteURI) ? newLocation : this.siteURI + newLocation;
                    coordinator.reportRedirect(node.url, redirectUri);
                    if (allowEnqueue(node, redirectUri)) {
                        queue.offer(new CrawledNode(redirectUri, coordinator.getNextId(), node.level, node.parentId));
                    }
                }
            } else {
                coordinator.reportFailureLink(node.url);
            }
        }
    }

    private boolean allowEnqueue(CrawledNode node, String link) {
        return coordinator.allowLink(link) && node.level + 1 <= this.maxSiteHeight;
    }

    private void taskCompletedAwardBudget() {
        this.workBudget = Math.min(MAX_BUDGET, this.workBudget + EARNED_BUDGET);
    }
}
