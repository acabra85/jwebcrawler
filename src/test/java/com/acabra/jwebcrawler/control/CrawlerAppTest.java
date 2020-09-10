package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.service.DownloadService;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.net.ssl.SSLSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CrawlerAppTest {

    private static final PriorityQueue<CrawledNode> EMPTY_PQ = new PriorityQueue<>();

    DownloadService downloadService = Mockito.mock(DownloadService.class);

    @BeforeEach
    public void setup(){
        Mockito.when(downloadService.download("http://localhost:8000/"))
                .thenReturn(getFutureResponseOF("site/index.html"));
        Mockito.when(downloadService.download("http://localhost:8000/index.html"))
                .thenReturn(getFutureResponseOF("site/index.html"));
        Mockito.when(downloadService.download("http://localhost:8000/a2.html"))
                .thenReturn(getFutureResponseOF("site/a2.html"));
        Mockito.when(downloadService.download("http://localhost:8000/a3.html"))
                .thenReturn(getFutureResponseOF("site/a3.html"));
        Mockito.when(downloadService.download("http://localhost:8000/a4.html"))
                .thenReturn(getFutureResponseOF("site/a4.html"));
        Mockito.when(downloadService.download("http://localhost:8000/a5.html"))
                .thenReturn(getFutureResponseOF("site/a5.html"));
        Mockito.when(downloadService.download("http://localhost:8000/a6.html"))
                .thenReturn(getFutureResponseOF("site/a6.html"));
        Mockito.when(downloadService.download("http://localhost:8000/a7.html"))
                .thenReturn(getFutureResponseOF("site/a7.html"));
        Mockito.when(downloadService.download("http://localhost:8000/a8.html"))
                .thenReturn(getFutureResponseOF("site/a8.html"));
        Mockito.when(downloadService.download("http://localhost:8000/a9.html"))
                .thenReturn(getFutureEmptyResponse());
    }

    @Test
    public void crawl_site_test() {
        int expectedSiteMaxHeight = 6;
        int maxSiteNodeLinks = 2;
        CrawlerApp crawlerApp = CrawlerApp.newBuilder()
                .withDomain("http://localhost:8000/")
                .withSleepWorkerTime(0.1)
                .withWorkerCount(4)
                .withMaxTreeSiteHeight(expectedSiteMaxHeight)
                .withMaxSiteNodeLinks(maxSiteNodeLinks)
                .build();
        CrawlSiteResponse crawlSiteResponse = crawlerApp.crawlSite(downloadService);
        Map<Long, PriorityQueue<CrawledNode>> graph = crawlSiteResponse.getGraph();

        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/index.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a2.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a3.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a4.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a5.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a6.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a7.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a8.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a9.html");

        Assertions.assertEquals(crawlSiteResponse.getTotalFailures(), 1);
        Assertions.assertEquals(crawlSiteResponse.getTotalRedirects(), 0);
        MatcherAssert.assertThat(calculateActualMaxChildren(graph), Matchers.lessThanOrEqualTo(maxSiteNodeLinks));
        MatcherAssert.assertThat(calculateActualHeight(graph), Matchers.lessThanOrEqualTo(expectedSiteMaxHeight));
    }

    @Test
    public void crawl_site_with_timeout_test() {
        int expectedSiteMaxHeight = 2;
        int maxSiteNodeLinks = 2;
        CrawlerApp crawlerApp = CrawlerApp.newBuilder()
                .withDomain("http://localhost:8000/")
                .withSleepWorkerTime(0.75)
                .withWorkerCount(1)
                .withMaxTreeSiteHeight(expectedSiteMaxHeight)
                .withMaxSiteNodeLinks(maxSiteNodeLinks)
                .withMaxExecutionTime(15)
                .build();
        CrawlSiteResponse crawlSiteResponse = crawlerApp.crawlSite(downloadService);
        Map<Long, PriorityQueue<CrawledNode>> graph = crawlSiteResponse.getGraph();

        Mockito.verify(downloadService, Mockito.atLeastOnce()).download("http://localhost:8000/");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a2.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a3.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a4.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/index.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a5.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a6.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a7.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a8.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a9.html");

        Assertions.assertEquals(crawlSiteResponse.getTotalFailures(), 0);
        Assertions.assertEquals(crawlSiteResponse.getTotalRedirects(), 0);
        MatcherAssert.assertThat(calculateActualMaxChildren(graph), Matchers.lessThanOrEqualTo(maxSiteNodeLinks));
        MatcherAssert.assertThat(calculateActualHeight(graph), Matchers.lessThanOrEqualTo(expectedSiteMaxHeight));
    }

    @Test
    public void crawl_site_with_max_1_child_test() {
        int maxTreeSiteHeight = 3;
        int maxSiteNodeLinks = 1;
        CrawlerApp crawlerApp = CrawlerApp.newBuilder()
                .withDomain("http://localhost:8000/")
                .withSleepWorkerTime(0.75)
                .withWorkerCount(1)
                .withMaxTreeSiteHeight(maxTreeSiteHeight)
                .withMaxSiteNodeLinks(maxSiteNodeLinks)
                .withMaxExecutionTime(15)
                .build();
        CrawlSiteResponse crawlSiteResponse = crawlerApp.crawlSite(downloadService);
        Map<Long, PriorityQueue<CrawledNode>> graph = crawlSiteResponse.getGraph();

        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a2.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/a3.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a4.html");
        Mockito.verify(downloadService, Mockito.times(1)).download("http://localhost:8000/index.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a5.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a6.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a7.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a8.html");
        Mockito.verify(downloadService, Mockito.times(0)).download("http://localhost:8000/a9.html");

        Assertions.assertEquals(crawlSiteResponse.getTotalFailures(), 0);
        Assertions.assertEquals(crawlSiteResponse.getTotalRedirects(), 0);
        MatcherAssert.assertThat(calculateActualMaxChildren(graph), Matchers.lessThanOrEqualTo(maxSiteNodeLinks));
        MatcherAssert.assertThat(calculateActualHeight(graph), Matchers.lessThanOrEqualTo(maxTreeSiteHeight));
    }

    private int calculateActualHeight(Map<Long, PriorityQueue<CrawledNode>> graph) {
        int height = 0;
        Stack<CrawledNode> q = new Stack<>();
        q.push(graph.get(-1L).remove());
        while (!q.isEmpty()) {
            CrawledNode pop = q.pop();
            height = Math.max(height, pop.level);
            PriorityQueue<CrawledNode> pq = graph.getOrDefault(pop.id, EMPTY_PQ);
            while (!pq.isEmpty()) q.push(pq.remove());
        }
        return height;
    }

    private int calculateActualMaxChildren(Map<Long, PriorityQueue<CrawledNode>> graph) {
        return graph.values().stream().mapToInt(Collection::size).max().orElse(0);
    }

    private CompletableFuture<HttpResponse<String>> getFutureResponseOF(String fileName) {
        return CompletableFuture.completedFuture(successHTMLResponseOf(getResourceAsText(fileName)));
    }

    private CompletableFuture<HttpResponse<String>> getFutureEmptyResponse() {
        return CompletableFuture.completedFuture(notFoundHTMLResponse());
    }

    private HttpResponse<String> notFoundHTMLResponse() {
        return buildHttpResponse("", "", 404, Map.of("content-type", List.of("text/html")));
    }

    private String getResourceAsText(String fileName) {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = CrawlerAppTest.class.getClassLoader().getResourceAsStream(fileName);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = null;
            while((line = br.readLine())!=null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        return "";
    }

    private HttpResponse<String> successHTMLResponseOf(String body) {
        return buildHttpResponse("", body, 200, Map.of("content-type", List.of("text/html")));
    }

    public HttpResponse<String> buildHttpResponse(String uri, String body, int statusCode, Map<String, List<String>> headers) {
        return new HttpResponse<>() {
            @Override public int statusCode() { return statusCode;}
            @Override public HttpRequest request() { return null;}
            @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty();}
            @Override public HttpHeaders headers() { return HttpHeaders.of(headers, (s, s2) -> true);}
            @Override public String body() { return body;}
            @Override public Optional<SSLSession> sslSession() { return Optional.empty();}
            @Override public URI uri() { return URI.create(uri);}
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_2;}
        };
    }
}
