package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.service.DownloadService;
import com.acabra.jwebcrawler.view.CrawlerReporter;
import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class CrawlerApp {

    private final Logger logger = LoggerFactory.getLogger(CrawlerApp.class);
    private final String siteURI;
    private final long startedAt = System.currentTimeMillis();
    private final CrawlerAppConfig config;

    CrawlerApp(CrawlerAppConfig config) {
        this.siteURI = config.siteUri;
        this.config = config;
    }

    public static CrawlerApp of(String... args) {
        return new CrawlerAppBuilder().of(args).build();
    }

    public static CrawlerAppBuilder newBuilder() {
        return new CrawlerAppBuilder();
    }

    private void evaluateTimeout(CrawlerCoordinator coordinator) {
        new Thread(() -> {
            try {
                Thread.sleep(config.timeout);
                logger.info("Application has reached timeout ... requesting workers to stop");
                coordinator.requestJobDone();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }).start();
    }

    protected CrawlSiteResponse crawlSite(DownloadService downloadService) {
        logger.info(String.format("Will attempt crawl for pages of SubDomain :<%s>", this.siteURI));

        BlockingQueue<CrawledNode> queue = new LinkedBlockingQueue<>();

        //create a lock for the queue
        ReentrantLock queueLock = new ReentrantLock(true);

        // enqueue the first website
        final CrawlerCoordinator coordinator = new CrawlerCoordinator();

        queue.offer(new CrawledNode(config.startUri, coordinator.getNextId()));

        ExecutorService executorService = Executors.newFixedThreadPool(config.workerCount);
        List<Runnable> tasks = buildTasks(queue, queueLock, coordinator, downloadService);
        CompletableFuture<?>[] completableFutures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, executorService))
                .toArray(CompletableFuture[]::new);

        if (config.isStoppable) evaluateTimeout(coordinator);
        CompletableFuture.allOf(completableFutures).join();
        executorService.shutdown();
        double totalRunningInSeconds = (System.currentTimeMillis() - this.startedAt) / 1000.0d;
        return new CrawlSiteResponse(coordinator.getGraph(), coordinator.getTotalRedirects(), coordinator.getTotalFailures(),
                totalRunningInSeconds);
    }

    private List<Runnable> buildTasks(BlockingQueue<CrawledNode> queue, ReentrantLock queueLock, CrawlerCoordinator coordinator, DownloadService downloadService) {
        List<Runnable> tasks = new ArrayList<>();
        IntStream.range(0, this.config.workerCount).forEach(i ->
                tasks.add(CrawlWorker.of(queue, coordinator,
                        this.siteURI,
                        config.sleepTime,
                        queueLock,
                        this.config.isStoppable,
                        this.config.maxChildLinks,
                        this.config.siteHeight,
                        downloadService))
        );
        return tasks;
    }

    public void start() {
        CrawlSiteResponse siteResponse = crawlSite(new DownloadService());
        CrawlerReporter.report(this.config.reportToFile, siteResponse, this.siteURI);
    }

    public int getRequestedThreads() {
        return this.config.workerCount;
    }

    public long getThreadSleepTime() {
        return this.config.sleepTime;
    }

    public int getMaxChildren() {
        return this.config.maxChildLinks;
    }

    public int getMaxSiteHeight() {
        return this.config.siteHeight;
    }

    public long getMaxExecutionTime() {
        return this.config.timeout;
    }

}
