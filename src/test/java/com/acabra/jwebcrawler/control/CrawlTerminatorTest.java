package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.*;

class CrawlTerminatorTest {

    @Test
    public void should_terminate_requests() {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        CrawlerCoordinator coordinatorMock = Mockito.mock(CrawlerCoordinator.class);
        CrawlTerminator underTest = new CrawlTerminator(new LinkedBlockingQueue<>(), ex, coordinatorMock, 3, 100L);

        underTest.run();

        Mockito.verify(coordinatorMock, Mockito.times(1)).requestJobDone();

        MatcherAssert.assertThat(underTest.getTotalPillsOffered(), Matchers.is(3));
    }

    @Test
    public void should_fail_offering_pills() {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        CrawlerCoordinator coordinatorMock = Mockito.mock(CrawlerCoordinator.class);
        CrawlTerminator underTest = new CrawlTerminator(new MyTestBlockingQueue(), ex, coordinatorMock, 3, 100L);

        underTest.run();

        Mockito.verify(coordinatorMock, Mockito.times(1)).requestJobDone();

        MatcherAssert.assertThat(underTest.getTotalPillsOffered(), Matchers.is(0));
    }

    @Test
    public void should_attempt_shutdown_executor() {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        CrawlerCoordinator coordinatorMock = Mockito.mock(CrawlerCoordinator.class);

        ex.shutdown();
        CrawlTerminator underTest = new CrawlTerminator(new LinkedBlockingQueue<>(), ex, coordinatorMock, 3, 100L);

        underTest.run();

        Mockito.verify(coordinatorMock, Mockito.times(1)).requestJobDone();
        MatcherAssert.assertThat(underTest.getTotalPillsOffered(), Matchers.is(3));
    }

    @Test
    public void should_terminate_executor_upon_exception() {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        CrawlerCoordinator coordinatorMock = Mockito.mock(CrawlerCoordinator.class);
        Mockito.doThrow(new RuntimeException("exception")).when(coordinatorMock).requestJobDone();

        CrawlTerminator underTest = new CrawlTerminator(new LinkedBlockingQueue<>(), ex, coordinatorMock, 3, 100L);

        underTest.run();

        Mockito.verify(coordinatorMock, Mockito.times(1)).requestJobDone();
        MatcherAssert.assertThat(underTest.getTotalPillsOffered(), Matchers.is(0));
        MatcherAssert.assertThat(ex.isShutdown(), Matchers.is(true));
    }

    static class MyTestBlockingQueue extends LinkedBlockingQueue<CrawledNode> {
        @Override
        public boolean offer(CrawledNode crawledNode) {
            return false;
        }
    }
}