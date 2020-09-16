package com.acabra.jwebcrawler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DownloadService<E> implements Downloader<E> {

    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);
    private final HttpClient client;
    private static final HttpResponse.BodyHandler<String> STRING_BODY_HANDLER = HttpResponse.BodyHandlers.ofString();

    public DownloadService(String siteURI) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4)).build();
        this.client.sendAsync(buildHttpRequest(siteURI), STRING_BODY_HANDLER)
                .handle((r, ex) -> r).join(); //warm up connection ignore results
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<E> download(String url) {
        return client.sendAsync(buildHttpRequest(url), STRING_BODY_HANDLER)
                .handle((res, ex) -> {
                    if(res == null) {
                        logger.error(ex.getMessage());
                    }
                    return (E) res;
                });
    }

    /**
     * Defaults a GET http request for the given url
     * @param url a URI will be created for this request based on it.
     * @return
     */
    private HttpRequest buildHttpRequest(String url) {
        return HttpRequest.newBuilder().uri(URI.create(url)).build();
    }
}
