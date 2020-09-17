package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.service.DownloadService;
import com.acabra.jwebcrawler.service.Downloader;
import com.acabra.jwebcrawler.view.CrawlerReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class CrawlerApp {

    public static final long POISON_PILL_ID = Integer.MIN_VALUE;
    public static final CrawledNode POISON_PILL = new CrawledNode("", POISON_PILL_ID);
    private static final double PERCENTAGE_CONSUMERS = 0.8;
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

    protected CrawlSiteResponse crawlSite(Supplier<Downloader<HttpResponse<String>>> supplier) {
        logger.info(String.format("Will attempt crawl for pages of SubDomain :<%s>", this.config.siteURI));


        //allow other 80% of capacity of the executor for Producers.
        ExecutorService executorService = Executors.newFixedThreadPool(Math.max(2, config.workerCount));

        final CrawlerCoordinator coordinator = new CrawlerCoordinator(executorService);

        BlockingQueue<CrawledNode> queue = new LinkedBlockingQueue<>(){{
            // enqueue the first website
            add(new CrawledNode(config.startUri, coordinator.getNextId()));
        }};

        int totalConsumers = Double.valueOf(
                Math.max(1, Math.floor(this.config.workerCount * PERCENTAGE_CONSUMERS))
        ).intValue();
        logger.info("total consumer crawlers: " + totalConsumers);

        List<Runnable> tasks = buildTasks(queue, coordinator, supplier, totalConsumers);
        CompletableFuture<?>[] completableFutures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, executorService))
                .toArray(CompletableFuture[]::new);

        dispatchCrawlTerminator(queue, executorService, coordinator, totalConsumers);

        CompletableFuture.allOf(completableFutures).join();
        executorService.shutdown();

        double totalRunningInSeconds = (System.currentTimeMillis() - this.startedAt) / 1000.0d;
        return new CrawlSiteResponse(this.config.siteURI, coordinator.getGraph(),
                coordinator.getTotalRedirects(),
                coordinator.getTotalFailures(),
                coordinator.getTotalEnqueueRejections(),
                totalRunningInSeconds,
                this.config.workerCount);
    }

    private void dispatchCrawlTerminator(BlockingQueue<CrawledNode> queue, ExecutorService executorService,
                                         CrawlerCoordinator coordinator, int totalConsumers) {
        new Thread(
                new CrawlTerminator(queue, executorService, coordinator, totalConsumers, this.config.timeout)
            ).start();//dispatch crawl terminator
    }

    private List<Runnable> buildTasks(BlockingQueue<CrawledNode> queue, CrawlerCoordinator coordinator,
                                      Supplier<Downloader<HttpResponse<String>>> supplier, int totalConsumers) {
        List<Runnable> tasks = new ArrayList<>();
        IntStream.range(0, totalConsumers).forEach(i ->
                tasks.add(new CrawlConsumerWorker(queue, coordinator, supplier.get(), this.config.copy()))
        );
        return tasks;
    }

    public void start() {
        CrawlSiteResponse siteResponse = crawlSite(() -> DownloadService.of(this.config.siteURI));
        new CrawlerReporter().report(this.config.reportToFile, siteResponse, "" + System.currentTimeMillis());
    }

    public CrawlerAppConfig getConfig() {
        return this.config;
    }
}
