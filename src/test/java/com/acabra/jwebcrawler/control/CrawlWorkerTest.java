package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.service.DownloadService;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

class CrawlWorkerTest {

    private DownloadService downloadServiceMock = Mockito.mock(DownloadService.class);
    private CrawlerCoordinator coordinator;
    private ReentrantLock lock;
    private LinkedBlockingQueue<CrawledNode> queue;

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
        this.lock = new ReentrantLock();
        this.coordinator = new CrawlerCoordinator();
    }

    @Test
    public void should_return_unique_sub_domain_links() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder
                .newBuilder("http://mysite.com/")
                .withMaxSiteNodeLinks(0) //dont limit child nodes
                .build();
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, lock, downloadServiceMock, defaults);
        List<String> expected = List.of("http://mysite.com/index.html",
                "http://mysite.com/a5.html",
                "http://mysite.com/a7.html",
                "http://mysite.com/a8.html");
        List<String> actualLinks = underTest.extractLinks(getHtmlWithLinks(), "http://mysite.com");

        Mockito.verify(downloadServiceMock, Mockito.never()).download(Mockito.anyString());

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
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, lock, downloadServiceMock, defaults);
        List<String> expected = List.of("http://mysite.com/index.html", "http://mysite.com/a5.html");
        List<String> actualLinks = underTest.extractLinks(getHtmlWithLinks(), "http://mysite.com");

        Mockito.verify(downloadServiceMock, Mockito.never()).download(Mockito.anyString());

        Assertions.assertEquals(actualLinks.size(), expected.size());
        IntStream.range(0, actualLinks.size()).forEach(i ->
                MatcherAssert.assertThat(actualLinks.get(i), Matchers.is(expected.get(i))));
    }

    @Test
    public void should_terminate_after_coordinator_request_job_done_stoppable_worker() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.5)
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();

        coordinator.requestJobDone();
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, new ReentrantLock(), downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.never()).download(Mockito.anyString());

        Assertions.assertTrue(defaults.isStoppable);
        Assertions.assertFalse(underTest.isEndInterrupted());
        MatcherAssert.assertThat(underTest.getIdleBudget(), Matchers.is(CrawlWorker.MAX_IDLE_BUDGET));
    }

    @Test
    public void should_ignore_coordinator_request_job_done_non_stoppable_worker_terminates_empty_budget() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.01)
                .build();

        coordinator.requestJobDone();
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, new ReentrantLock(), downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.times(7)).download(Mockito.anyString());

        Assertions.assertFalse(defaults.isStoppable);
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
        MatcherAssert.assertThat(underTest.getIdleBudget(), Matchers.is(0));
    }

    @Test
    public void should_not_limit_request_download_no_site_limit() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.01)
                .withMaxTreeSiteHeight(0) //dont limit site depth
                .build();

        coordinator.requestJobDone();
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, new ReentrantLock(), downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.times(7)).download(Mockito.anyString());

        Assertions.assertFalse(defaults.isStoppable);
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
        MatcherAssert.assertThat(underTest.getIdleBudget(), Matchers.is(0));
    }

    @Test
    public void should_limit_request_download_since_limit_site_set() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.01)
                .withMaxTreeSiteHeight(1) //dont limit site depth
                .build();

        coordinator.requestJobDone();
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, new ReentrantLock(), downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.times(6)).download(Mockito.anyString());

        Assertions.assertFalse(defaults.isStoppable);
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
        MatcherAssert.assertThat(underTest.getIdleBudget(), Matchers.is(0));
    }

    @Test
    public void should_terminate_coordinator_request_job_done_on_non_empty_queue() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.75)
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();

        CrawlerCoordinator coordinatorMock = Mockito.mock(CrawlerCoordinator.class);
        Mockito.when(coordinatorMock.isJobDone()).thenReturn(true);

        CrawlWorker underTest = new CrawlWorker(queue, coordinatorMock, new ReentrantLock(), downloadServiceMock, defaults);
        queue.offer(new CrawledNode(defaults.startUri, 0L));

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(coordinatorMock, Mockito.times(1)).isJobDone();
        Mockito.verify(downloadServiceMock, Mockito.never()).download(Mockito.anyString());

        Assertions.assertTrue(defaults.isStoppable);
        Assertions.assertEquals(1, queue.size());
        Assertions.assertFalse(underTest.isEndInterrupted());
        MatcherAssert.assertThat(underTest.getIdleBudget(), Matchers.is(CrawlWorker.MAX_IDLE_BUDGET));
    }

    @Test
    public void should_terminate_coordinator_request_job_done_on_non_empty_queue_idle_worker() throws InterruptedException {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(1.5)
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();
        ReentrantLock sharedLock = new ReentrantLock();
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        sharedLock.lock(); //prevent worker from taking nodes from the queue forcing sleep time.

        CrawlWorker underTest = new CrawlWorker(queue, coordinator, sharedLock, downloadServiceMock, defaults);
        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor());

        Thread.sleep(100L);
        coordinator.requestJobDone();
        sharedLock.unlock();

        Mockito.verify(downloadServiceMock, Mockito.never()).download(Mockito.anyString());

        Assertions.assertTrue(defaults.isStoppable);
        Assertions.assertFalse(underTest.isEndInterrupted());
        Assertions.assertEquals(1, queue.size());
        MatcherAssert.assertThat(underTest.getIdleBudget(), Matchers.lessThan(CrawlWorker.MAX_IDLE_BUDGET));
    }

    @Test
    public void should_interrupt_non_stoppable_worker_while_awaits_download() throws InterruptedException {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://delayed-website.com")
                .withSleepWorkerTime(0.75)
                .build();
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, new ReentrantLock(), downloadServiceMock, defaults);
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        Thread workerThread = buildAndStartThread(underTest);
        Thread.sleep(100L);
        workerThread.interrupt();

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download(defaults.startUri);

        Assertions.assertFalse(defaults.isStoppable);
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertTrue(underTest.isEndInterrupted());
        MatcherAssert.assertThat(underTest.getIdleBudget(), Matchers.is(CrawlWorker.MAX_IDLE_BUDGET));
    }

    @Test
    public void should_interrupt_stoppable_worker_while_awaits_download() throws InterruptedException {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://delayed-website.com")
                .withSleepWorkerTime(0.75)
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();
        queue.offer(new CrawledNode(defaults.startUri, 0L));

        CrawlWorker underTest = new CrawlWorker(queue, coordinator, new ReentrantLock(), downloadServiceMock, defaults);
        Thread workerThread = buildAndStartThread(underTest);
        Thread.sleep(100L);
        workerThread.interrupt();

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download(defaults.startUri);

        Assertions.assertTrue(defaults.isStoppable);
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertTrue(underTest.isEndInterrupted());
        MatcherAssert.assertThat(underTest.getIdleBudget(), Matchers.is(CrawlWorker.MAX_IDLE_BUDGET));
    }

    @Test
    public void should_terminate_idle_budget_depleted_non_stop_on_request_worker() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.01)
                .build();
        int expectedIdleBudget = 0;
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, lock, downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.never()).download(Mockito.anyString());

        Assertions.assertFalse(defaults.isStoppable);
        Assertions.assertFalse(underTest.isEndInterrupted());
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertEquals(expectedIdleBudget, underTest.getIdleBudget());
    }

    @Test
    public void should_terminate_idle_budget_depleted_stop_on_request_worker() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://mysite.com/")
                .withSleepWorkerTime(0.01)
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();
        int expectedIdleBudget = 0;
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, lock, downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.never()).download(Mockito.anyString());

        Assertions.assertTrue(defaults.isStoppable);
        Assertions.assertFalse(underTest.isEndInterrupted());
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertEquals(expectedIdleBudget, underTest.getIdleBudget());
    }

    @Test
    public void should_terminate_exception_thrown_stoppable_worker() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://throwexeptionsite.com/")
                .withSleepWorkerTime(0.01)
                .withMaxExecutionTime(0.25) //indicates the worker is stoppable
                .build();
        int expectedIdleBudget = CrawlWorker.MAX_IDLE_BUDGET;
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, lock, downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Assertions.assertTrue(defaults.isStoppable);
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
        Assertions.assertEquals(expectedIdleBudget, underTest.getIdleBudget());
    }

    @Test
    public void should_terminate_exception_thrown_non_stoppable_worker() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://throwexeptionsite.com/")
                .withSleepWorkerTime(0.01)
                .build();
        int expectedIdleBudget = CrawlWorker.MAX_IDLE_BUDGET;
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        CrawlWorker underTest = new CrawlWorker(queue, coordinator, lock, downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Assertions.assertFalse(defaults.isStoppable);
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
        Assertions.assertEquals(expectedIdleBudget, underTest.getIdleBudget());
    }

    @Test
    public void should_release_queue_lock_if_held_interrupted_exception_non_stoppable_worker() throws InterruptedException {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://throwexeptionsite.com/")
                .withSleepWorkerTime(0.01)
                .build();
        int expectedIdleBudget = CrawlWorker.MAX_IDLE_BUDGET;
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        LinkedBlockingQueue<CrawledNode> spyQueue = Mockito.spy(queue);
        Mockito.doThrow(new InterruptedException("Forcing interruption of thread"))
                .when(spyQueue).take();

        CrawlWorker underTest = new CrawlWorker(spyQueue, coordinator, lock, downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Assertions.assertFalse(defaults.isStoppable);
        Assertions.assertFalse(queue.isEmpty());
        Assertions.assertTrue(underTest.isEndInterrupted());
        Assertions.assertEquals(expectedIdleBudget, underTest.getIdleBudget());
    }

    @Test
    public void should_release_queue_lock_if_held_interrupted_exception_stoppable_worker() throws InterruptedException {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://throwexeptionsite.com/")
                .withSleepWorkerTime(0.01)
                .withMaxExecutionTime(0.25)
                .build();
        int expectedIdleBudget = CrawlWorker.MAX_IDLE_BUDGET;
        queue.offer(new CrawledNode(defaults.startUri, 0L));
        LinkedBlockingQueue<CrawledNode> spyQueue = Mockito.spy(queue);
        Mockito.doThrow(new InterruptedException("Forcing interruption of thread"))
                .when(spyQueue).take();

        CrawlWorker underTest = new CrawlWorker(spyQueue, coordinator, lock, downloadServiceMock, defaults);

        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.never()).download(Mockito.anyString());
        Mockito.verify(spyQueue, Mockito.times(1)).take();

        Assertions.assertTrue(defaults.isStoppable);
        Assertions.assertFalse(queue.isEmpty());
        Assertions.assertTrue(underTest.isEndInterrupted());
        Assertions.assertEquals(expectedIdleBudget, underTest.getIdleBudget());
    }

    @Test
    public void should_enqueue_work_right_before_idle_unit_depletion_and_then_request_job_done() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder.newBuilder("http://othersite.com/")
                .withSleepWorkerTime(0.01)
                .withMaxExecutionTime(0.25)
                .build();
        AtomicInteger workUnits = new AtomicInteger(CrawlWorker.MAX_IDLE_BUDGET + 1);

        Mockito.when(downloadServiceMock.download(defaults.startUri))
                .thenReturn(TestUtils.getFutureEmptyResponse());

        CrawlerCoordinator coordinatorSpy = Mockito.spy(coordinator);
        Mockito.when(coordinatorSpy.isJobDone()).thenAnswer((invocationOnMock -> {
            if(workUnits.get() == 0) { // request worker job complete
                return true;
            }
            workUnits.decrementAndGet();
            if (workUnits.get() == 1) { //once the worker has only 1 idle unit left put work in the queue
                queue.offer(new CrawledNode(defaults.startUri, 0L));
            }
            return false;
        }));

        CrawlWorker underTest = new CrawlWorker(queue, coordinatorSpy, new ReentrantLock(), downloadServiceMock, defaults);
        CompletableFuture.runAsync(underTest, Executors.newSingleThreadExecutor()).join();

        Mockito.verify(downloadServiceMock, Mockito.times(1)).download(Mockito.anyString());
        Mockito.verify(coordinatorSpy, Mockito.times(CrawlWorker.MAX_IDLE_BUDGET + CrawlWorker.AWARDED_UNITS)).isJobDone();

        Assertions.assertTrue(defaults.isStoppable);
        Assertions.assertTrue(queue.isEmpty());
        Assertions.assertFalse(underTest.isEndInterrupted());
        MatcherAssert.assertThat(underTest.getIdleBudget(), Matchers.is(CrawlWorker.AWARDED_UNITS)); //worker awarded 2 units.
    }
}