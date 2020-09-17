package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class CrawlerCoordinatorTest {

    private CrawlerCoordinator underTest;
    private ExecutorService ex;

    @BeforeEach
    public void setup() {
        ex = Executors.newSingleThreadExecutor();
        underTest = new CrawlerCoordinator(ex);
    }

    @Test
    void allowLink() {

        underTest.reportFailureLink("failure.com");
        underTest.processNode(new CrawledNode("visited01.com", 0L));
        underTest.processNode(new CrawledNode("failure2.com", 1L, 1, 0L));
        underTest.reportFailureLink("failure2.com");

        MatcherAssert.assertThat(underTest.allowLink("a.com"), Matchers.is(true));
        MatcherAssert.assertThat(underTest.allowLink("visited01.com"), Matchers.is(false));
        MatcherAssert.assertThat(underTest.allowLink("failure.com"), Matchers.is(false));
        MatcherAssert.assertThat(underTest.allowLink("failure2.com"), Matchers.is(false));
    }

    @Test
    public void should_reject_task_executor_shutdown(){
        ex.shutdown();
        CrawlProducerWorker producerMock = Mockito.mock(CrawlProducerWorker.class);
        MatcherAssert.assertThat(underTest.getTotalEnqueueRejections(), Matchers.is(0L));

        underTest.dispatchProducer(producerMock);

        MatcherAssert.assertThat(underTest.getTotalEnqueueRejections(), Matchers.is(1L));
    }
}