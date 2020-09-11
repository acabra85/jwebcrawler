package com.acabra.jwebcrawler.control;


import com.acabra.jwebcrawler.dto.DefaultCrawlerConfiguration;
import com.acabra.jwebcrawler.utils.JsonHelper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CrawlerAppBuilderTest {

    @Test
    public void should_fail_null_domain_given() {
        Exception exception = Assertions.assertThrows(NullPointerException.class, CrawlerApp::of);
        Assertions.assertTrue(exception.getMessage().startsWith("Sub-domain not found"));
    }

    @Test
    public void should_fail_empty_spaces_as_domain_given() {
        Exception exception = Assertions.assertThrows(NullPointerException.class, () -> CrawlerApp.of("   "));
        Assertions.assertTrue(exception.getMessage().startsWith("Sub-domain not found"));
    }

    @Test
    public void should_fail_empty_domain_given() {
        Exception exception = Assertions.assertThrows(NullPointerException.class, () -> CrawlerApp.of(""));
        Assertions.assertTrue(exception.getMessage().startsWith("Sub-domain not found"));
    }

    @Test
    public void should_fail_invalid_thread_count_given() {
        Assertions.assertThrows(NumberFormatException.class, () ->
                CrawlerApp.of("http://www.mydomain.com", "ggggggggggggg"));
    }

    @Test
    public void should_fail_invalid_thread_sleep_time_given() {
        Assertions.assertThrows(NumberFormatException.class, () ->
                CrawlerApp.of("http://www.mydomain.com", "4", "dedf"));
    }

    @Test
    public void should_pass_valid_arguments_1() {
        CrawlerApp app = CrawlerApp.of("http://127.0.0.1:8000/");
        MatcherAssert.assertThat(app, Matchers.notNullValue());
        MatcherAssert.assertThat(app.getRequestedThreads(), Matchers.is(Integer.MAX_VALUE));
        MatcherAssert.assertThat(app.getThreadSleepTime(), Matchers.is(1000L));
    }

    @Test
    public void should_pass_valid_arguments_2() {
        CrawlerApp app = CrawlerApp.of("http://127.0.0.1:8000/", "3");
        MatcherAssert.assertThat(app, Matchers.notNullValue());
        MatcherAssert.assertThat(app.getRequestedThreads(), Matchers.is(3));
        MatcherAssert.assertThat(app.getThreadSleepTime(), Matchers.is(1000L));
    }

    @Test
    public void should_pass_valid_arguments_3() {
        CrawlerApp app = CrawlerApp.of("http://127.0.0.1:8000/", "3", "1.5");
        MatcherAssert.assertThat(app, Matchers.notNullValue());
        MatcherAssert.assertThat(app.getRequestedThreads(), Matchers.is(3));
        MatcherAssert.assertThat(app.getThreadSleepTime(), Matchers.is(1500L));
    }

    @Test
    public void should_pass_valid_arguments_3_number_threads_adjusted() {
        CrawlerApp app = CrawlerApp.of("http://127.0.0.1:8000/", "-1");
        MatcherAssert.assertThat(app, Matchers.notNullValue());
        MatcherAssert.assertThat(app.getRequestedThreads(), Matchers.is(1));
        MatcherAssert.assertThat(app.getThreadSleepTime(), Matchers.is(1000L));
    }

    @Test
    public void should_pass_valid_arguments_4_sleep_thread_time_adjusted() {
        CrawlerApp app = CrawlerApp.of("http://127.0.0.1:8000/", "4", "11000");
        MatcherAssert.assertThat(app, Matchers.notNullValue());
        MatcherAssert.assertThat(app.getRequestedThreads(), Matchers.is(4));
        MatcherAssert.assertThat(app.getThreadSleepTime(), Matchers.is(10000L));
    }

    @Test
    public void should_have_defaults() {
        CrawlerApp underTest = new CrawlerAppBuilder()
                .withDomain("http://127.0.0.1:8000/").build();
        DefaultCrawlerConfiguration expected = JsonHelper.getInstance().fromJsonFile("config.json", DefaultCrawlerConfiguration.class);

        Assertions.assertEquals(underTest.getRequestedThreads(), expected.workerCount);
        Assertions.assertEquals(underTest.getMaxChildren(), expected.maxSiteNodeLinks);
        Assertions.assertEquals(underTest.getMaxSiteHeight(), expected.siteHeight);
        Assertions.assertEquals(underTest.getThreadSleepTime(), expected.sleepTime * 1000);
        Assertions.assertEquals(underTest.getMaxExecutionTime(), expected.maxExecutionTime);
    }
}