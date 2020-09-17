package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class CrawlTerminator implements Runnable {

    private static final long MILLIS_BEFORE_FORCE_TERMINATE = 800;
    private final BlockingQueue<CrawledNode> queue;
    private final ExecutorService ex;
    private final CrawlerCoordinator coordinator;
    private final int totalConsumers;
    private final Logger logger = LoggerFactory.getLogger(CrawlTerminator.class);
    private final long timeout;
    private int pillsOffered = 0;

    public CrawlTerminator(BlockingQueue<CrawledNode> queue, ExecutorService executorService,
                           CrawlerCoordinator coordinator, int totalConsumers, long timeout) {
        this.queue = queue;
        this.ex = executorService;
        this.coordinator = coordinator;
        this.totalConsumers = totalConsumers;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(this.timeout);
            logger.info("Application has reached timeout ... requesting workers to stop");
            coordinator.requestJobDone();
            IntStream.range(0, totalConsumers)
                    .forEach(i-> {
                        logger.info("sending poison pill #: " + (i+1));
                        if(!queue.offer(CrawlerApp.POISON_PILL)) {
                            logger.info("unable to enqueue poison pill, threads will interrupted");
                        } else {
                            ++pillsOffered;
                        }
                    });
            if(!ex.isShutdown()) {
                ex.shutdown();
                if(!ex.awaitTermination(MILLIS_BEFORE_FORCE_TERMINATE, TimeUnit.MILLISECONDS)) {
                    ex.shutdownNow();
                }
            }
        } catch (Exception e) {
            ex.shutdownNow();
            logger.error(e.getMessage());
        }
    }

    public int getTotalPillsOffered() {
        return this.pillsOffered;
    }
}
