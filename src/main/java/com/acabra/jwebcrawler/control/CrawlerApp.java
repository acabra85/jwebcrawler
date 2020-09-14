package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.service.DownloadService;
import com.acabra.jwebcrawler.service.Downloader;
import com.acabra.jwebcrawler.view.CrawlerReporter;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlerApp {

    private final Logger logger = LoggerFactory.getLogger(CrawlerApp.class);
    private final long startedAt = System.currentTimeMillis();
    private final CrawlerAppConfig config;

    CrawlerApp(CrawlerAppConfig config) {
        this.config = config;
        logger.info("Given Configuration :" + config.toString());
    }

    public static CrawlerApp of(String... args) {
        return new CrawlerApp(CrawlerAppConfigBuilder.newBuilder(args).build());
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

    protected CrawlSiteResponse crawlSite(Supplier<Downloader<HttpResponse<String>>> supplier) {
        logger.info(String.format("Will attempt crawl for pages of SubDomain :<%s>", this.config.siteURI));

        BlockingQueue<CrawledNode> queue = new LinkedBlockingQueue<>();

        //create a lock for the queue
        ReentrantLock queueLock = new ReentrantLock(true);

        // enqueue the first website
        ExecutorService executorService = Executors.newFixedThreadPool(config.workerCount);

        final CrawlerCoordinator coordinator = new CrawlerCoordinator();

        queue.offer(new CrawledNode(config.startUri, coordinator.getNextId()));

        List<Runnable> tasks = buildTasks(queue, queueLock, coordinator, supplier);
        CompletableFuture<?>[] completableFutures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, executorService))
                .toArray(CompletableFuture[]::new);

        if (config.isStoppable) evaluateTimeout(coordinator);
        CompletableFuture.allOf(completableFutures).join();
        executorService.shutdown();
        double totalRunningInSeconds = (System.currentTimeMillis() - this.startedAt) / 1000.0d;
        return new CrawlSiteResponse(this.config.siteURI, coordinator.getGraph(),
                coordinator.getTotalRedirects(),
                coordinator.getTotalFailures(),
                totalRunningInSeconds);
    }

    private List<Runnable> buildTasks(BlockingQueue<CrawledNode> queue, ReentrantLock queueLock,
                                      CrawlerCoordinator coordinator,
                                      Supplier<Downloader<HttpResponse<String>>> supplier) {
        List<Runnable> tasks = new ArrayList<>();
        IntStream.range(0, this.config.workerCount).forEach(i ->
                tasks.add(new CrawlConsumerWorker(queue, coordinator, queueLock, supplier.get(), this.config.copy()))
        );
        return tasks;
    }

    public void start() {
        CrawlSiteResponse siteResponse = crawlSite(DownloadService::new);
        new CrawlerReporter().report(this.config.reportToFile, siteResponse, "" + System.currentTimeMillis());
    }

    public CrawlerAppConfig getConfig() {
        return this.config;
    }
}
