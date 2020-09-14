package com.acabra.jwebcrawler.service;

import java.util.concurrent.CompletableFuture;

public interface Downloader<T> {

    /**
     * Downloads the contents of the given uri
     * @return returns a completable future with the contents of the response
     */
    CompletableFuture<T> download(String uri);
}
