package com.acabra.jwebcrawler.service;

import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class DownloadServiceTest {

    private DownloadService<HttpResponse<String>> underTest;

    @Test
    void download_should_succeed() throws ExecutionException, InterruptedException {
        String siteURI = "https://www.google.com/";
        underTest = DownloadService.of(siteURI);
        HttpResponse<String> download = underTest.download(siteURI).get();
        MatcherAssert.assertThat(download.statusCode(), Matchers.is(200));
    }

    @Test
    void download_should_fail() throws ExecutionException, InterruptedException {
        String siteURI = "http://somerandomwebsite-ooo097.com/";
        underTest = DownloadService.of(siteURI);

        MatcherAssert.assertThat(underTest.download(siteURI).get(), Matchers.nullValue());
    }
}