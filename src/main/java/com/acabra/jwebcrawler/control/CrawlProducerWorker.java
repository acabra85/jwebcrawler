package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.model.ProcessedResponse;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlProducerWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CrawlProducerWorker.class);
    private final List<String> EMPTY_RESPONSE = Collections.emptyList();
    private final CrawlerCoordinator coordinator;
    private final CrawlerAppConfig config;
    private final BlockingQueue<CrawledNode> queue;
    private final HttpResponse<String> httpResponse;
    private final CrawledNode node;


    public CrawlProducerWorker(CrawlerCoordinator coordinator, CrawlerAppConfig config, BlockingQueue<CrawledNode> queue,
                               CrawledNode node, HttpResponse<String> httpResponse) {

        this.coordinator = coordinator;
        this.config = config;
        this.queue = queue;
        this.httpResponse = httpResponse;
        this.node = node;
    }

    private ProcessedResponse processHTTPResponse(CrawledNode node, HttpResponse<String> httpResponse) {
        String uri = node.url;
        if (null != httpResponse && isResponseHtml(httpResponse.headers())) {
            int statusCode = httpResponse.statusCode();
            List<String> links = EMPTY_RESPONSE;
            if (statusCode == 200) {
                links = extractLinks(String.valueOf(httpResponse.body()), this.config.siteURI);
            } else if (statusCode == 301 || statusCode == 302) {
                List<String> location = httpResponse.headers().map().get("Location");
                if (location!= null && location.size() > 0) {
                    links = location;
                    //logger.info(String.format("[%s] permanently moved to [%s] call code: %d", uri, links.get(0), statusCode));
                }
            } else {
                coordinator.reportFailureLink(uri);
                //logger.info(String.format("Failed GET uri: [%s] call code: %d", uri, statusCode));
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

    private void enqueueLocalDomainLinksFound(CrawledNode node, ProcessedResponse pResponse) {
        if (pResponse.success) {
            if (pResponse.statusCode == 200) {
                pResponse.links.forEach(link ->
                    attemptEnqueue(link, node.level + 1, node.buildChild(link, coordinator.getNextId()))
                );
            } else if (pResponse.statusCode == 301 || pResponse.statusCode == 302) {
                // requeue the page with the given redirection link
                if (pResponse.links != null && pResponse.links.size() > 0) {
                    String newLocation = pResponse.links.get(0);
                    String redirectUri = newLocation.startsWith(this.config.siteURI) ? newLocation : this.config.siteURI + newLocation;
                    coordinator.reportRedirect(node.url, redirectUri);
                    attemptEnqueue(redirectUri, node.level, node.redirection(redirectUri, coordinator.getNextId()));
                }
            } else {
                coordinator.reportFailureLink(node.url);
            }
        }
    }

    private boolean attemptEnqueue(String redirectUri, int level, CrawledNode redirection) {
        if (coordinator.isJobDone()) {
            return false;
        }
        if (allowEnqueue(level, redirectUri)) {
            return queue.offer(redirection);
        }
        return false;
    }

    private boolean allowEnqueue(int level, String link) {
        if (this.config.siteHeight > 0) {
            return this.config.siteHeight >= level && coordinator.allowLink(link);
        }
        return coordinator.allowLink(link);
    }

    void process() {
        ProcessedResponse processedResponse = processHTTPResponse(this.node, this.httpResponse);
        enqueueLocalDomainLinksFound(node, processedResponse);
    }

    @Override
    public void run() {
        try {
            process();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
