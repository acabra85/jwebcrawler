package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.service.DownloadService;
import com.acabra.jwebcrawler.view.CrawlerReporter;
import com.acabra.jwebcrawler.utils.UrlValidator;
import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class CrawlerApp {

    private final Logger logger = LoggerFactory.getLogger(CrawlerApp.class);
    private final static Integer DEFAULT_T_SLEEP_TIME = 1;
    private final static Long MIN_SLEEP_TIME = 100L;
    private final static Long MAX_SLEEP_TIME = 10000L;
    private final String siteURI;
    private final long startedAt = System.currentTimeMillis();
    private final int requestedThreads;
    private final String startPage;
    private final long requestedSleepTime;
    private final long timeout;
    private final int maxSiteHeight;
    private final int maxChildren;

    private CrawlerApp(URL rootUrl, int requestedThreads, long requestedSleepTime, long timeoutInSeconds,
                       int maxSiteNodeLinks, int siteHeight) {
        this.siteURI = String.format("%s://%s", rootUrl.getProtocol(),
                rootUrl.getAuthority());
        this.startPage = rootUrl.getPath();
        this.requestedThreads = Math.max(requestedThreads, 1);
        this.requestedSleepTime = Math.min(MAX_SLEEP_TIME, Math.max(requestedSleepTime, MIN_SLEEP_TIME));
        this.timeout = timeoutInSeconds;
        this.maxChildren = maxSiteNodeLinks;
        this.maxSiteHeight = siteHeight;
    }


    static CrawlerApp of(String domain, int workerCount, double sleepTime, int siteHeight,
                                 int maxSiteNodeLinks, double maxExecutionTime) {
        URL rootUrl = UrlValidator.buildURL(domain);
        return new CrawlerApp(rootUrl, workerCount, Double.valueOf(Math.min(sleepTime, 10.0) * 1000).longValue(),
                Double.valueOf(maxExecutionTime * 1000).longValue(), maxSiteNodeLinks, siteHeight);
    }

    public static CrawlerApp of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime, String maxExecutionTime) {
        return new CrawlerAppBuilder().withDomain(subDomainStr)
                .withWorkerCount(Integer.parseInt(reqThreadCountStr))
                .withSleepWorkerTime(Double.parseDouble(reqSleepThreadTime))
                .build();
    }

    public static CrawlerApp of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime) {
        return of(subDomainStr, reqThreadCountStr, reqSleepThreadTime, "0");
    }

    public static CrawlerApp of(String subDomainStr, String reqThreadCountStr) {
        return of(subDomainStr, reqThreadCountStr, DEFAULT_T_SLEEP_TIME.toString());
    }

    public static CrawlerApp of(String subDomainStr) {
        return of(subDomainStr, String.valueOf(Integer.MAX_VALUE));
    }

    public static CrawlerAppBuilder newBuilder() {
        return new CrawlerAppBuilder();
    }

    public void start(DownloadService downloadService) {
        CrawlSiteResponse siteResponse = crawlSite(downloadService);
        String report = CrawlerReporter.buildReport(siteResponse);
        writeToFile(report);
    }

    private void writeToFile(String report) {
        try {
            StringBuilder siteName = new StringBuilder();
            for (char c : siteURI.split("//")[1].toLowerCase().toCharArray()) {
                if (Character.isAlphabetic(c) || Character.isDigit(c)) {
                    siteName.append(c);
                } else {
                    siteName.append("_");
                }
            }
            String filePath = "target/results/crawl-" + siteName + "-" + System.currentTimeMillis() + ".txt";
            new File("target/results").mkdirs();
            Files.write(Paths.get(filePath), report.getBytes());
        } catch (IOException e) {
            logger.error("Error while writing the report to file: " + e.getMessage());
        }
    }

    public CrawlSiteResponse crawlSite(DownloadService downloadService) {
        logger.info(String.format("Will attempt crawl for pages of SubDomain :<%s>", this.siteURI));
        int totalWorkers = Math.min(requestedThreads, Runtime.getRuntime().availableProcessors());
        BlockingQueue<CrawledNode> queue = new LinkedBlockingQueue<>();

        //create a lock for the queue
        ReentrantLock queueLock = new ReentrantLock(true);

        // enqueue the first website
        final CrawlerCoordinator coordinator = new CrawlerCoordinator(this.siteURI);

        queue.offer(new CrawledNode(this.siteURI + this.startPage, coordinator.getNextId()));

        ExecutorService executorService = Executors.newFixedThreadPool(totalWorkers);
        boolean stoppable = this.timeout > 0;
        List<Runnable> tasks = buildTasks(totalWorkers, queue, coordinator, this.requestedSleepTime,
                queueLock, stoppable, downloadService);
        CompletableFuture<?>[] completableFutures = tasks.stream()
                .map(task -> CompletableFuture.runAsync(task, executorService))
                .toArray(CompletableFuture[]::new);

        if (stoppable) evaluateTimeout(coordinator);
        CompletableFuture.allOf(completableFutures).join();
        executorService.shutdown();
        double totalRunningInSeconds = (System.currentTimeMillis() - this.startedAt) / 1000.0d;
        return new CrawlSiteResponse(coordinator.getGraph(), coordinator.getTotalRedirects(), coordinator.getTotalFailures(),
                totalRunningInSeconds);
    }

    private void evaluateTimeout(CrawlerCoordinator coordinator) {
        new Thread(() -> {
            try {
                Thread.sleep(timeout);
                logger.info("Application has reached timeout ... requesting workers to stop");
                coordinator.requestJobDone();
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }).start();
    }

    private List<Runnable> buildTasks(int size, BlockingQueue<CrawledNode> queue, CrawlerCoordinator coordinator,
                                      long sleepTime, ReentrantLock queueLock, boolean stoppable,
                                      DownloadService downloadService) {
        List<Runnable> tasks = new ArrayList<>();
        IntStream.range(0, size).forEach(i ->
            tasks.add(Worker.of(queue, coordinator, this.siteURI, sleepTime, queueLock, stoppable, this.maxChildren,
                    this.maxSiteHeight, downloadService))
        );
        return tasks;
    }

    public int getRequestedThreads() {
        return requestedThreads;
    }

    public long getThreadSleepTime() {
        return this.requestedSleepTime;
    }

    public int getMaxChildren() {
        return this.maxChildren;
    }

    public int getMaxSiteHeight() {
        return this.maxSiteHeight;
    }

    public long getMaxExecutionTime() {
        return this.timeout;
    }
}
