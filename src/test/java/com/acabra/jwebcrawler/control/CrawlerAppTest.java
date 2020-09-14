package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.service.DownloadService;
import com.acabra.jwebcrawler.service.Downloader;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.function.Supplier;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CrawlerAppTest {

    private static final PriorityQueue<CrawledNode> EMPTY_PQ = new PriorityQueue<>();

    DownloadService downloadServiceMock;

    Supplier<Downloader<HttpResponse<String>>> downloadServiceSupplier;

    @BeforeEach
    public void setup(){
        downloadServiceMock = Mockito.mock(DownloadService.class);
        downloadServiceSupplier = () -> downloadServiceMock;
        TestUtils.cleanReportsFolder();
        Mockito.when(downloadServiceMock.download("http://localhost:8000/"))
                .thenReturn(TestUtils.getFutureResponseOF("site/index.html"));
        Mockito.when(downloadServiceMock.download("http://localhost:8000/index.html"))
                .thenReturn(TestUtils.getFutureResponseOF("site/index.html"));
        Mockito.when(downloadServiceMock.download("http://localhost:8000/a2.html"))
                .thenReturn(TestUtils.getFutureResponseOF("site/a2.html"));
        Mockito.when(downloadServiceMock.download("http://localhost:8000/a3.html"))
                .thenReturn(TestUtils.getFutureResponseOF("site/a3.html"));
        Mockito.when(downloadServiceMock.download("http://localhost:8000/a4.html"))
                .thenReturn(TestUtils.getFutureResponseOF("site/a4.html"));
        Mockito.when(downloadServiceMock.download("http://localhost:8000/a5.html"))
                .thenReturn(TestUtils.getFutureResponseOF("site/a5.html"));
        Mockito.when(downloadServiceMock.download("http://localhost:8000/a6.html"))
                .thenReturn(TestUtils.getFutureResponseOF("site/a6.html"));
        Mockito.when(downloadServiceMock.download("http://localhost:8000/a7.html"))
                .thenReturn(TestUtils.getFutureResponseOF("site/a7.html"));
        Mockito.when(downloadServiceMock.download("http://localhost:8000/a8.html"))
                .thenReturn(TestUtils.getFutureResponseOF("site/a8.html"));
        Mockito.when(downloadServiceMock.download("http://localhost:8000/a9.html"))
                .thenReturn(TestUtils.getFutureEmptyResponse());
        Mockito.when(downloadServiceMock.download("http://localhost:8000/a6redirect.html"))
                .thenReturn(TestUtils.getFutureRedirectResponse());
        Mockito.when(downloadServiceMock.download("http://localhost:8000/mypdffile.pdf"))
                .thenReturn(TestUtils.getFuturePDFResponse());
        Mockito.when(downloadServiceMock.download("http://delayed-website.com"))
                .thenAnswer((invocationOnMock) -> {
                    Thread.sleep(5000L);
                    return TestUtils.getFutureEmptyResponse();
                });
    }

    @Test
    public void crawl_site_test() {
        int expectedSiteMaxHeight = 6;
        int maxSiteNodeLinks = 2;
        CrawlerApp crawlerApp = new CrawlerApp(CrawlerAppConfigBuilder.newBuilder("http://localhost:8000/")
                .withSleepWorkerTime(0.1)
                .withWorkerCount(4)
                .withMaxTreeSiteHeight(expectedSiteMaxHeight)
                .withMaxSiteNodeLinks(maxSiteNodeLinks)
                .build());
        CrawlSiteResponse crawlSiteResponse = crawlerApp.crawlSite(downloadServiceSupplier);
        Map<Long, PriorityQueue<CrawledNode>> graph = crawlSiteResponse.getGraph();

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/index.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a2.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a3.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a4.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a5.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a6.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a6redirect.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a7.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a8.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a9.html");

        Assertions.assertEquals(crawlSiteResponse.getTotalFailures(), 1);
        Assertions.assertEquals(crawlSiteResponse.getTotalRedirects(), 1);
        Assertions.assertTrue(graph.containsKey(CrawledNode.ROOT_NODE_PARENT_ID));
        MatcherAssert.assertThat(calculateActualMaxChildren(graph), Matchers.lessThanOrEqualTo(maxSiteNodeLinks));
        MatcherAssert.assertThat(calculateActualHeight(graph), Matchers.lessThanOrEqualTo(expectedSiteMaxHeight));
    }

    @Test
    public void crawl_site_with_timeout_test() {
        int expectedSiteMaxHeight = 2;
        int maxSiteNodeLinks = 2;
        CrawlerApp crawlerApp = new CrawlerApp(CrawlerAppConfigBuilder.newBuilder("http://localhost:8000/")
                .withSleepWorkerTime(0.1)
                .withWorkerCount(1)
                .withMaxTreeSiteHeight(expectedSiteMaxHeight)
                .withMaxSiteNodeLinks(maxSiteNodeLinks)
                .build());
        CrawlSiteResponse crawlSiteResponse = crawlerApp.crawlSite(downloadServiceSupplier);
        Map<Long, PriorityQueue<CrawledNode>> graph = crawlSiteResponse.getGraph();

        Mockito.verify(downloadServiceMock, Mockito.atLeastOnce()).download("http://localhost:8000/");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a2.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a3.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a4.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/index.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a5.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a6.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a7.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a8.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a9.html");

        Assertions.assertEquals(crawlSiteResponse.getTotalFailures(), 0);
        Assertions.assertEquals(crawlSiteResponse.getTotalRedirects(), 0);
        Assertions.assertTrue(graph.containsKey(CrawledNode.ROOT_NODE_PARENT_ID));
        MatcherAssert.assertThat(calculateActualMaxChildren(graph), Matchers.lessThanOrEqualTo(maxSiteNodeLinks));
        MatcherAssert.assertThat(calculateActualHeight(graph), Matchers.lessThanOrEqualTo(expectedSiteMaxHeight));
    }

    @Test
    public void crawl_site_with_max_1_child_test() {
        int maxTreeSiteHeight = 3;
        int maxSiteNodeLinks = 1;
        CrawlerApp crawlerApp = new CrawlerApp(CrawlerAppConfigBuilder.newBuilder("http://localhost:8000/")
                .withSleepWorkerTime(0.1)
                .withWorkerCount(1)
                .withMaxTreeSiteHeight(maxTreeSiteHeight)
                .withMaxSiteNodeLinks(maxSiteNodeLinks)
                .build());
        CrawlSiteResponse crawlSiteResponse = crawlerApp.crawlSite(downloadServiceSupplier);
        Map<Long, PriorityQueue<CrawledNode>> graph = crawlSiteResponse.getGraph();

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/a2.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a3.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a4.html");
        Mockito.verify(downloadServiceMock, Mockito.times(1)).download("http://localhost:8000/index.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a5.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a6.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a7.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a8.html");
        Mockito.verify(downloadServiceMock, Mockito.times(0)).download("http://localhost:8000/a9.html");

        Assertions.assertEquals(crawlSiteResponse.getTotalFailures(), 0);
        Assertions.assertEquals(crawlSiteResponse.getTotalRedirects(), 0);
        Assertions.assertTrue(graph.containsKey(CrawledNode.ROOT_NODE_PARENT_ID));
        MatcherAssert.assertThat(calculateActualMaxChildren(graph), Matchers.lessThanOrEqualTo(maxSiteNodeLinks));
        MatcherAssert.assertThat(calculateActualHeight(graph), Matchers.lessThanOrEqualTo(maxTreeSiteHeight));
    }

    @Test
    public void should_interrupt_execution() {
        String domain = "http://delayed-website.com";
        CrawlerApp underTest = new CrawlerApp(CrawlerAppConfigBuilder.newBuilder(domain)
                .withWorkerCount(1)
                .withSleepWorkerTime(3)
                .withMaxExecutionTime(1)
                .build());

        CrawlSiteResponse actualResponse = underTest.crawlSite(downloadServiceSupplier);
        Map<Long, PriorityQueue<CrawledNode>> graph = actualResponse.getGraph();

        CrawledNode expectedRootNode = new CrawledNode(domain, 0L);

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download(domain);

        MatcherAssert.assertThat(actualResponse.getTotalRedirects(), Matchers.is(0));
        MatcherAssert.assertThat(actualResponse.getTotalFailures(), Matchers.is(1)); // no time to fail since it was shut down
        MatcherAssert.assertThat(actualResponse.getTotalTime(), Matchers.lessThanOrEqualTo(6.0));
        Assertions.assertEquals(graph.size(), 1);
        Assertions.assertTrue(graph.containsKey(CrawledNode.ROOT_NODE_PARENT_ID));
        MatcherAssert.assertThat(graph.get(CrawledNode.ROOT_NODE_PARENT_ID), Matchers.contains(expectedRootNode));
        MatcherAssert.assertThat(Objects.requireNonNull(graph.get(CrawledNode.ROOT_NODE_PARENT_ID).peek()).url,
                Matchers.is(domain));
        Assertions.assertFalse(TestUtils.reportFileWasCreated(underTest.getConfig().siteURI));
    }

    @Test
    public void should_build_report_file() {
        String domain = "http://localhost:8000/";
        CrawlerApp underTest = new CrawlerApp(CrawlerAppConfigBuilder.newBuilder(domain)
                .withWorkerCount(1)
                .withSleepWorkerTime(0.1)
                .withMaxExecutionTime(10)
                .withReportToFile(true)
                .build());
        underTest.start();

        Assertions.assertTrue(TestUtils.reportFileWasCreated(underTest.getConfig().siteURI));
    }

    @Test
    public void should_not_build_report_file() {
        String domain = "http://localhost:8000";
        CrawlerApp underTest = new CrawlerApp(CrawlerAppConfigBuilder.newBuilder(domain)
                .withWorkerCount(1)
                .withSleepWorkerTime(0.1)
                .build());
        underTest.start();
        Assertions.assertFalse(TestUtils.reportFileWasCreated(underTest.getConfig().siteURI));
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
}
