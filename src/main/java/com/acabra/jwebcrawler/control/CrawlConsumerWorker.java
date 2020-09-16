package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.service.Downloader;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represent the individual worker with the task of downloading the content of the given uri
 */
public class CrawlConsumerWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CrawlConsumerWorker.class);
    private volatile boolean endInterrupted = false;

    private final BlockingQueue<CrawledNode> queue;
    private final CrawlerCoordinator coordinator;

    private Downloader<HttpResponse<String>> downloadService;
    private final CrawlerAppConfig config;

    public CrawlConsumerWorker(BlockingQueue<CrawledNode> queue, CrawlerCoordinator coordinator,
                               Downloader<HttpResponse<String>> downloadService, CrawlerAppConfig config) {
        this.queue = queue;
        this.coordinator = coordinator;
        this.downloadService = downloadService;
        this.config = config;
    }

    public boolean isEndInterrupted() {
        return endInterrupted;
    }

    @Override
    public void run() {
        try {
            while (true) {
                final CrawledNode node = queue.take();
                if (node.id == CrawlerApp.POISON_PILL_ID) {
                    logger.info("drinking poison pill ...");
                    return;
                }
                processNode(node);
            }
        } catch (InterruptedException ie) {
            this.endInterrupted = true;
            logger.error("CrawlConsumerWorker interrupted: ", ie);
            Thread.currentThread().interrupt();
        }  catch (Exception exception) {
            logger.error("Error thrown while run loop: ", exception);
            exception.printStackTrace();
        }
    }

    private void processNode(CrawledNode node) throws InterruptedException {
        String resolvedUrl = coordinator.resolve(node.url);
        if (coordinator.allowLink(resolvedUrl)) {
            coordinator.processNode(node);
            this.downloadService.download(resolvedUrl).thenAccept(httpResponse ->
                new Thread(
                    new CrawlProducerWorker(this.coordinator, this.config, this.queue, node, httpResponse)
                ).start()).join();
        }
        if (this.config.sleepTime > 0) {
            Thread.sleep(this.config.sleepTime);
        }
    }
}
