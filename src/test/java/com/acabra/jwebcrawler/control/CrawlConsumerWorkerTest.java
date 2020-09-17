package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.service.DownloadService;
import com.acabra.jwebcrawler.service.Downloader;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CrawlConsumerWorkerTest {

    private Downloader<HttpResponse<String>> downloadServiceMock = Mockito.mock(DownloadService.class);
    private CrawlerCoordinator coordinator;
    private LinkedBlockingQueue<CrawledNode> queue;

    private Thread buildAndStartThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        return thread;
    }

    @BeforeEach
    public void setup() {
        Mockito.when(downloadServiceMock.download("http://mysite.com/"))
                .thenReturn(TestUtils.getFutureResponseOF("site/mysite.html"));
        Mockito.when(downloadServiceMock.download("http://mysite.com/mysite.html"))
                .thenReturn(TestUtils.getFutureResponseOF("site/mysite.html"));
        Mockito.when(downloadServiceMock.download("http://mysite.com/mysite5.html"))
                .thenReturn(TestUtils.getFutureEmptyResponse());
        Mockito.when(downloadServiceMock.download("http://mysite.com/mysite7.html"))
                .thenReturn(TestUtils.getFutureEmptyResponse());
        Mockito.when(downloadServiceMock.download("http://mysite.com/mysite8.html"))
                .thenReturn(TestUtils.getFutureRedirectResponseTo("/myfile.pdf"));
        Mockito.when(downloadServiceMock.download("http://mysite.com/myfile.pdf"))
                .thenReturn(TestUtils.getFuturePDFResponse());
        Mockito.when(downloadServiceMock.download("http://delayed-website.com"))
                .thenAnswer((invocationOnMock) -> {
                    Thread.sleep(5000L);
                    System.out.println("answering after delay");
                    return TestUtils.getFutureEmptyResponse();
                });
        Mockito.when(downloadServiceMock.download("http://throwexeptionsite.com/")).
                thenThrow(new RuntimeException("failure on download service"));
        this.queue = new LinkedBlockingQueue<>();
        this.coordinator = new CrawlerCoordinator(Executors.newSingleThreadExecutor());
    }

    @Test
    public void should_terminate_after_drinking_poison_pill() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.5)
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();

        CrawlConsumerWorker underTest = new CrawlConsumerWorker(queue, coordinator, downloadServiceMock, defaults);

        buildAndStartThread(() -> {
            try {
                Thread.sleep(10L);
                queue.offer(CrawlerApp.POISON_PILL);
            }
            catch (InterruptedException ie) { System.out.println(ie.getMessage());}
        });
        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.atMostOnce()).download(Mockito.anyString()); //warm up call

        Assertions.assertFalse(underTest.isEndInterrupted());
    }

    @Test
    public void should_terminate_after_drinking_poison_pill_no_sleep_time() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();

        CrawlConsumerWorker underTest = new CrawlConsumerWorker(queue, coordinator, downloadServiceMock, defaults);

        buildAndStartThread(() -> {
            try {
                Thread.sleep(10L);
                queue.offer(CrawlerApp.POISON_PILL);
            }
            catch (InterruptedException ie) { System.out.println(ie.getMessage());}
        });
        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.atMostOnce()).download(Mockito.anyString()); //warm up call

        Assertions.assertFalse(underTest.isEndInterrupted());
    }

    @Test
    public void should_not_limit_request_download_no_site_limit() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0)
                .withMaxTreeSiteHeight(0) //dont limit site depth
                .build();

        queue.offer(new CrawledNode(defaults.startUri, 0L));
        CrawlConsumerWorker underTest = new CrawlConsumerWorker(queue, coordinator, downloadServiceMock, defaults);

        buildAndStartThread(()-> {
            try {
                Thread.sleep(3000L);
                queue.offer(CrawlerApp.POISON_PILL);
            } catch (Exception e) { System.out.println(e.getMessage()); }
        });

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.times(5)).download(Mockito.anyString());

        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
    }

    @Test
    public void should_limit_request_download_since_limit_site_set() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.01)
                .withMaxTreeSiteHeight(1) //limit site height
                .build();

        queue.offer(new CrawledNode(defaults.startUri, 0L));
        CrawlConsumerWorker underTest = new CrawlConsumerWorker(queue, coordinator, downloadServiceMock, defaults);

        buildAndStartThread(()-> {
            try {
                Thread.sleep(3000L);
                queue.offer(CrawlerApp.POISON_PILL);
            } catch (Exception e) { System.out.println(e.getMessage()); }
        });

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.times(5)).download(Mockito.anyString());

        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
    }

    @Test
    public void should_interrupt_non_stoppable_worker_while_awaits_download() throws InterruptedException {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://delayed-website.com")
                .withSleepWorkerTime(0.75)
                .build();
        CrawlConsumerWorker underTest = new CrawlConsumerWorker(queue, coordinator, downloadServiceMock, defaults);
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        Thread workerThread = buildAndStartThread(underTest);
        Thread.sleep(100L);
        workerThread.interrupt();

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download(defaults.startUri);

        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertTrue(underTest.isEndInterrupted());
    }

    @Test
    public void should_interrupt_stoppable_worker_while_awaits_download() throws InterruptedException {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://delayed-website.com")
                .withSleepWorkerTime(0.75)
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();
        queue.offer(new CrawledNode(defaults.startUri, 0L));

        CrawlConsumerWorker underTest = new CrawlConsumerWorker(queue, coordinator, downloadServiceMock, defaults);
        Thread workerThread = buildAndStartThread(underTest);
        Thread.sleep(100L);
        workerThread.interrupt();

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download(defaults.startUri);

        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertTrue(underTest.isEndInterrupted());
    }

    @Test
    public void should_terminate_exception_thrown_stoppable_worker() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://throwexeptionsite.com/")
                .withSleepWorkerTime(0.01)
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();

        buildAndStartThread(()-> {
            try {
                Thread.sleep(3000L);
                queue.offer(CrawlerApp.POISON_PILL);
            } catch (Exception e) { System.out.println(e.getMessage()); }
        });

        queue.offer(new CrawledNode(defaults.startUri, 0L));
        CrawlConsumerWorker underTest = new CrawlConsumerWorker(queue, coordinator, downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download(Mockito.anyString());

        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
    }

    @Test
    public void should_terminate_exception_thrown_non_stoppable_worker() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://throwexeptionsite.com/")
                .withSleepWorkerTime(0.01)
                .build();
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        CrawlConsumerWorker underTest = new CrawlConsumerWorker(queue, coordinator, downloadServiceMock, defaults);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture.runAsync(underTest, executor).join();
        executor.shutdown();

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download(Mockito.anyString());

        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
    }

    @Test
    void should_stop_if_coordinator_requested() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.01)
                .withMaxTreeSiteHeight(1) //limit site height
                .build();

        queue.offer(new CrawledNode(defaults.startUri, 0L));
        coordinator.requestJobDone();
        CrawlConsumerWorker underTest = new CrawlConsumerWorker(queue, coordinator, downloadServiceMock, defaults);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture.runAsync(underTest, executor).join();
        executor.shutdown();

        MatcherAssert.assertThat(queue.size(), Matchers.is(1)); // job stop requested before taking from queue

    }
}