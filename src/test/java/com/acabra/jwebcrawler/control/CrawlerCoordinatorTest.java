package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrawlerCoordinatorTest {

    private CrawlerCoordinator underTest;

    @BeforeEach
    public void setup() {
        underTest = new CrawlerCoordinator();
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
}