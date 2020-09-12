package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.service.DownloadService;
import java.util.stream.Stream;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Represent the individual worker with the task of downloading the content of the given uri
 */
public class CrawlWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CrawlWorker.class);
    static final int MAX_IDLE_BUDGET = 8;
    static final int AWARDED_UNITS = 2;
    private volatile boolean endInterrupted = false;

    private final BlockingQueue<CrawledNode> queue;
    private final CrawlerCoordinator coordinator;
    private final ReentrantLock queueLock;
    private int idleBudget; //budget units when reaches zero allows graceful termination

    private final List<String> EMPTY_RESPONSE = Collections.emptyList();
    private DownloadService downloadService;
    private final CrawlerAppConfig config;

    public CrawlWorker(BlockingQueue<CrawledNode> queue, CrawlerCoordinator coordinator, ReentrantLock queueLock,
                DownloadService downloadService, CrawlerAppConfig config) {
        this.queue = queue;
        this.coordinator = coordinator;
        this.idleBudget = MAX_IDLE_BUDGET;
        this.queueLock = queueLock;
        this.downloadService = downloadService;
        this.config = config;
    }

    /**
     * Not thread safe, left for testing purposes after the work is finished
     * @return
     */
    public int getIdleBudget() {
        return idleBudget;
    }

    public boolean isEndInterrupted() {
        return endInterrupted;
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

    private ProcessedResponse processHTTPResponse(CrawledNode node, HttpResponse<String> httpResponse) {
        String uri = node.url;
        coordinator.processNode(node);
        if (null != httpResponse && isResponseHtml(httpResponse.headers())) {
            int statusCode = httpResponse.statusCode();
            List<String> links = EMPTY_RESPONSE;
            if (statusCode == 200) {
                links = extractLinks(String.valueOf(httpResponse.body()), this.config.siteURI);
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
            if (null != httpResponse) {
                logger.info(String.format("Content type is not text/html {%s}", uri));
            } else {
                logger.info("Error while retrieving content from url: " + uri);
            }
            coordinator.reportFailureLink(uri);
            return ProcessedResponse.ofError(node.url);
        }
    }

    private boolean isResponseHtml(HttpHeaders headers) {
        Optional<String> contentTypeHtml = headers.allValues("content-type").stream().filter(value -> value.contains("text/html")).findFirst();
        return contentTypeHtml.isPresent();
    }

    List<String> extractLinks(String htmlResponseBody, String siteURI) {
        Elements links = Jsoup.parse(htmlResponseBody, siteURI).select("a");
        return withLimit(links.stream()
                .map(element -> element.attr("abs:href"))
                .filter(link -> link.startsWith(siteURI))
                .distinct()).collect(Collectors.toList());
    }

    private Stream<String> withLimit(Stream<String> distinctLinks) {
        if (this.config.maxChildLinks > 0) {
            return distinctLinks.limit(this.config.maxChildLinks);
        }
        return distinctLinks;
    }

    private boolean canProcessQueue() {
        return queue.size() > 0 && this.queueLock.tryLock();
    }

    @Override
    public void run() {
        if(this.config.isStoppable) {
            runStoppable();
        } else {
            try {
                while (true) {
                    if (canProcessQueue()) {
                        readFromQueue();
                    } else if (this.idleBudget > 0) {
                        --this.idleBudget;
                        Thread.sleep(this.config.sleepTime);
                    } else {
                        logger.info("Idle Budget depleted");
                        return;
                    }
                }
            } catch (InterruptedException ie) {
                this.endInterrupted = true;
                logger.error("CrawlWorker interrupted: ", ie);
                Thread.currentThread().interrupt();
            }  catch (Exception exception) {
                logger.error("Error thrown while run loop: ", exception);
                exception.printStackTrace();
            } finally {
                if (this.queueLock.isHeldByCurrentThread()) {
                    this.queueLock.unlock();
                }
                logger.info("worker finished with an idle budget of: " + idleBudget);
            }
        }
    }

    private void runStoppable() {
        logger.info("Running stop-on-request worker");
        try {
            while (true) {
                if (coordinator.isJobDone()) {
                    logger.info("Coordinator requested job completion");
                    return;
                } else if (canProcessQueue()) {
                    readFromQueue();
                } else if (this.idleBudget > 0) {
                    --this.idleBudget;
                    Thread.sleep(this.config.sleepTime);
                } else {
                    logger.info("Idle Budget depleted");
                    return;
                }
            }
        }
        catch (InterruptedException ie) {
            this.endInterrupted = true;
            logger.error("CrawlWorker interrupted: ", ie);
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            logger.error("Error thrown while run loop: ", exception);
            exception.printStackTrace();
        } finally {
            if (this.queueLock.isHeldByCurrentThread()) {
                this.queueLock.unlock();
            }
            logger.info("worker finished with an idle budget of: " + idleBudget);
        }
    }

    private void readFromQueue() throws InterruptedException {
        final CrawledNode node = queue.take();
        this.queueLock.unlock();
        String resolve = coordinator.resolve(node.url);
        if (coordinator.allowLink(resolve)) {
            logger.info("request download of : " + resolve);
            this.downloadService.download(resolve)
                    .thenApply(resp -> processHTTPResponse(node, resp))
                    .thenAccept(pResponse -> enqueueLocalDomainLinksFound(node, pResponse))
                    .handle((r, e) -> {
                        this.taskCompletedAwardBudget();
                        return r;
                    })
                    .join();
        }
    }

    private void enqueueLocalDomainLinksFound(CrawledNode node, ProcessedResponse pResponse) {
        if (pResponse.success) {
            if (pResponse.statusCode == 200) {
                pResponse.links.forEach(link ->
                        enqueueIfAllowed(link, node.level + 1, node.buildChild(link, coordinator.getNextId())));
            } else if (pResponse.statusCode == 301 || pResponse.statusCode == 302) {
                // requeue the page with the given redirection link
                if (pResponse.links != null && pResponse.links.size() > 0) {
                    String newLocation = pResponse.links.get(0);
                    String redirectUri = newLocation.startsWith(this.config.siteURI) ? newLocation : this.config.siteURI + newLocation;
                    coordinator.reportRedirect(node.url, redirectUri);
                    enqueueIfAllowed(redirectUri, node.level, node.redirection(redirectUri, coordinator.getNextId()));
                }
            } else {
                coordinator.reportFailureLink(node.url);
            }
        }
    }

    private void enqueueIfAllowed(String redirectUri, int level, CrawledNode redirection) {
        if (allowEnqueue(level, redirectUri)) {
            queue.offer(redirection);
        }
    }

    private boolean allowEnqueue(int level, String link) {
        if (this.config.siteHeight > 0) {
            return coordinator.allowLink(link) && this.config.siteHeight >= level;
        }
        return coordinator.allowLink(link);
    }

    private void taskCompletedAwardBudget() {
        this.idleBudget = Math.min(MAX_IDLE_BUDGET, this.idleBudget + AWARDED_UNITS);
    }
}
