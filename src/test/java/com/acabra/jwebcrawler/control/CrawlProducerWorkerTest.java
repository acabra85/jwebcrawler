package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CrawlProducerWorkerTest {

    private CrawlerCoordinator coordinatorMock;

    private String getHtmlWithLinks() {
        return "<html lang=\"en\">\n" +
                "<body>\n" +
                "<a href=\"https://www.someexternalwebsite.com/\">some website</a>\n" +
                "<a href=\"/index.html\">index</a>\n" +
                "<div><a href=\"/a5.html\">a5</a></div>" +
                "<div><a href=\"/a7.html\">a7</a></div>" +
                "<div><a href=\"/a8.html\">a8</a></div>" +
                "<div><a href=\"/a8.html\">a8</a></div>" +
                "<div><a href=\"/a5.html\">a5</a></div>" +
                "</body>\n" +
                "</html>";
    }

    @BeforeEach
    public void setup() {
        coordinatorMock = Mockito.mock(CrawlerCoordinator.class);

        Mockito.when(coordinatorMock.reportFailureLink("http://2secs-await-url.com/"))
                .thenAnswer((invocationOnMock -> {
                    Thread.sleep(2000L);
                    return false;
                }));
    }

    @Test
    public void should_return_unique_sub_domain_links() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder
                .newBuilder("http://mysite.com/")
                .withMaxSiteNodeLinks(0) //dont limit child nodes
                .build();

        CrawlProducerWorker underTest = new CrawlProducerWorker(
                coordinatorMock, defaults, null, new CrawledNode(defaults.startUri, 0L), null);
        List<String> expected = List.of("http://mysite.com/index.html",
                "http://mysite.com/a5.html",
                "http://mysite.com/a7.html",
                "http://mysite.com/a8.html");

        List<String> actualLinks = underTest.extractLinks(getHtmlWithLinks(), "http://mysite.com");

        Mockito.verify(coordinatorMock, Mockito.never()).allowLink(Mockito.any());
        Mockito.verify(coordinatorMock, Mockito.never()).reportFailureLink(Mockito.anyString());

        Assertions.assertEquals(actualLinks.size(), expected.size());
        IntStream.range(0, actualLinks.size()).forEach(i ->
                MatcherAssert.assertThat(actualLinks.get(i), Matchers.is(expected.get(i))));
    }

    @Test
    public void should_return_unique_sub_domain_links_limited() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder
                .newBuilder("http://mysite.com/")
                .withMaxSiteNodeLinks(2)
                .build();
        CrawlProducerWorker underTest = new CrawlProducerWorker(
                coordinatorMock, defaults, null, new CrawledNode(defaults.startUri, 0L), null);
        List<String> expected = List.of("http://mysite.com/index.html", "http://mysite.com/a5.html");
        List<String> actualLinks = underTest.extractLinks(getHtmlWithLinks(), "http://mysite.com");


        Mockito.verify(coordinatorMock, Mockito.never()).allowLink(Mockito.anyString());
        Mockito.verify(coordinatorMock, Mockito.never()).reportFailureLink(Mockito.anyString());

        Assertions.assertEquals(actualLinks.size(), expected.size());
        IntStream.range(0, actualLinks.size()).forEach(i ->
                MatcherAssert.assertThat(actualLinks.get(i), Matchers.is(expected.get(i))));
    }

    @Test
    public void should_interrupt_producer() throws InterruptedException {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder
                .newBuilder("http://2secs-await-url.com/")
                .withMaxSiteNodeLinks(2)
                .build();

        CrawledNode startNode = new CrawledNode(defaults.startUri, 0L);
        BlockingQueue<CrawledNode> queue = new LinkedBlockingQueue<>();
        Thread thread = new Thread(new CrawlProducerWorker(coordinatorMock, defaults, queue, startNode, null));
        thread.start();
        Thread.sleep(100L);
        thread.interrupt();

        Mockito.verify(coordinatorMock, Mockito.never()).allowLink(Mockito.anyString());
        Mockito.verify(coordinatorMock, Mockito.times(1)).reportFailureLink(defaults.startUri);

        MatcherAssert.assertThat(queue.size(), Matchers.is(0));
    }

    @Test
    public void should_enqueue_items_parsed() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder
                .newBuilder("http://mysite.com/")
                .withMaxSiteNodeLinks(2)
                .build();


        Mockito.when(coordinatorMock.allowLink(Mockito.anyString()))
                .thenReturn(true);

        CrawledNode startNode = new CrawledNode(defaults.startUri, 0L);
        BlockingQueue<CrawledNode> queue = new LinkedBlockingQueue<>();
        HttpResponse<String> httpResponse = TestUtils.successHTMLResponseOf(getHtmlWithLinks());
        CrawlProducerWorker producer = new CrawlProducerWorker(coordinatorMock, defaults, queue, startNode,
                httpResponse);

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        CompletableFuture.runAsync(producer, executorService).join();

        Mockito.verify(coordinatorMock, Mockito.times(2)).allowLink(Mockito.anyString());
        Mockito.verify(coordinatorMock, Mockito.never()).reportFailureLink(Mockito.anyString());

        MatcherAssert.assertThat(queue.size(), Matchers.is(2));
    }

    @Test
    public void should_enqueue_no_items_as_not_allowed() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder
                .newBuilder("http://mysite.com/")
                .withMaxSiteNodeLinks(2)
                .build();

        Mockito.when(coordinatorMock.allowLink(Mockito.anyString()))
                .thenReturn(false);

        CrawledNode startNode = new CrawledNode(defaults.startUri, 0L);
        BlockingQueue<CrawledNode> queue = new LinkedBlockingQueue<>();
        HttpResponse<String> httpResponse = TestUtils.successHTMLResponseOf(getHtmlWithLinks());
        CrawlProducerWorker underTest = new CrawlProducerWorker(coordinatorMock, defaults, queue, startNode,
                httpResponse);

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        CompletableFuture.runAsync(underTest, executorService).join();

        Mockito.verify(coordinatorMock, Mockito.times(2)).allowLink(Mockito.anyString());
        Mockito.verify(coordinatorMock, Mockito.never()).reportFailureLink(Mockito.anyString());

        MatcherAssert.assertThat(queue.size(), Matchers.is(0));
    }

    @Test
    void should_stop_if_coordinator_requested() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder
                .newBuilder("http://mysite.com/")
                .withMaxSiteNodeLinks(2)
                .build();

        Mockito.when(coordinatorMock.isJobDone()).thenReturn(true);
        CrawledNode startNode = new CrawledNode(defaults.startUri, 0L);
        BlockingQueue<CrawledNode> queue = new LinkedBlockingQueue<>();
        HttpResponse<String> httpResponse = TestUtils.successHTMLResponseOf(getHtmlWithLinks());

        CrawlProducerWorker underTest = new CrawlProducerWorker(coordinatorMock, defaults, queue, startNode,
                httpResponse);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CompletableFuture.runAsync(underTest, executorService).join();
    }


}