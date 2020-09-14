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
    static final int MAX_IDLE_BUDGET = 8;
    static final int AWARDED_UNITS = 2;
    private volatile boolean endInterrupted = false;

    private final BlockingQueue<CrawledNode> queue;
    private final CrawlerCoordinator coordinator;
    private final ReentrantLock queueLock;
    private int idleBudget; //budget units when reaches zero allows graceful termination

    private Downloader<HttpResponse<String>> downloadService;
    private final CrawlerAppConfig config;

    public CrawlConsumerWorker(BlockingQueue<CrawledNode> queue, CrawlerCoordinator coordinator, ReentrantLock queueLock,
                               Downloader<HttpResponse<String>> downloadService, CrawlerAppConfig config) {
        this.queue = queue;
        this.coordinator = coordinator;
        this.idleBudget = MAX_IDLE_BUDGET;
        this.queueLock = queueLock;
        this.downloadService = downloadService;
        this.config = config;
    }

    /**
     * Not thread safe, left for testing purposes after the work is finished
     * @return
     */
    public int getIdleBudget() {
        return idleBudget;
    }

    public boolean isEndInterrupted() {
        return endInterrupted;
    }

    private boolean canProcessQueue() {
        return queue.size() > 0 && this.queueLock.tryLock();
    }

    @Override
    public void run() {
        if(this.config.isStoppable) {
            runStoppable();
        } else {
            try {
                while (true) {
                    if (canProcessQueue()) {
                        readFromQueue();
                    } else if (this.idleBudget > 0) {
                        --this.idleBudget;
                        Thread.sleep(this.config.sleepTime);
                    } else {
                        logger.info("Idle Budget depleted");
                        return;
                    }
                }
            } catch (InterruptedException ie) {
                this.endInterrupted = true;
                logger.error("CrawlConsumerWorker interrupted: ", ie);
                Thread.currentThread().interrupt();
            }  catch (Exception exception) {
                logger.error("Error thrown while run loop: ", exception);
                exception.printStackTrace();
            } finally {
                if (this.queueLock.isHeldByCurrentThread()) {
                    this.queueLock.unlock();
                }
                logger.info("worker finished with an idle budget of: " + idleBudget);
            }
        }
    }

    private void runStoppable() {
        logger.info("Running stop-on-request worker");
        try {
            while (true) {
                if (coordinator.isJobDone()) {
                    logger.info("Coordinator requested job completion");
                    return;
                } else if (canProcessQueue()) {
                    readFromQueue();
                } else if (this.idleBudget > 0) {
                    --this.idleBudget;
                    Thread.sleep(this.config.sleepTime);
                } else {
                    logger.info("Idle Budget depleted");
                    return;
                }
            }
        }
        catch (InterruptedException ie) {
            this.endInterrupted = true;
            logger.error("CrawlConsumerWorker interrupted: ", ie);
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            logger.error("Error thrown while run loop: ", exception);
            exception.printStackTrace();
        } finally {
            if (this.queueLock.isHeldByCurrentThread()) {
                this.queueLock.unlock();
            }
            logger.info("worker finished with an idle budget of: " + idleBudget);
        }
    }

    private void readFromQueue() throws InterruptedException {
        final CrawledNode node = queue.take();
        this.queueLock.unlock();
        String resolve = coordinator.resolve(node.url);
        if (coordinator.allowLink(resolve)) {
            logger.info("request download of : " + resolve);
            this.downloadService.download(resolve)
                    .thenAccept(resp -> {
                        new CrawlProducerWorker(this.coordinator, this.config, this.queue, node, resp).run();
                        taskCompletedAwardBudget();
                    }).join();
        }
    }

    private void taskCompletedAwardBudget() {
        this.idleBudget = Math.min(MAX_IDLE_BUDGET, this.idleBudget + AWARDED_UNITS);
    }
}
