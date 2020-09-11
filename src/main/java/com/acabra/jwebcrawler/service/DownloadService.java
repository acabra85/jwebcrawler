package com.acabra.jwebcrawler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DownloadService {

    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);
    private final HttpClient client;

    public DownloadService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4)).build();
    }

    public CompletableFuture<HttpResponse<String>> download(String url) {
        return client.sendAsync(buildHttpRequest(url), HttpResponse.BodyHandlers.ofString())
                .handle((res, ex) -> {
                    if(res == null) {
                        logger.error(ex.getMessage());
                    }
                    return res;
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
