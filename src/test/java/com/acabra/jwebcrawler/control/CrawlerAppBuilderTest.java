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
    public void should_fail_wrong_node_depth() {
        Assertions.assertThrows(NumberFormatException.class, () ->
            CrawlerApp.of("http://127.0.0.1:8000/", "4", "5", "10", "true", "sdas")
        );
    }

    @Test
    public void should_fail_wrong_site_height() {
        Assertions.assertThrows(NumberFormatException.class, () ->
            CrawlerApp.of("http://127.0.0.1:8000/", "4", "5", "10", "true", "15", "sdas")
        );
    }

    @Test
    public void should_pass_valid_arguments_1() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(1));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(1000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(0L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(10));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(6));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(false));
    }

    @Test
    public void should_pass_valid_arguments_2() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "3");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(3));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(1000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(0L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(10));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(6));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(false));
    }

    @Test
    public void should_pass_valid_arguments_3() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "3", "1.5");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(3));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(1500L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(0L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(10));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(6));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(false));
    }

    @Test
    public void should_pass_valid_arguments_3_number_threads_adjusted() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "-1");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(1));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(1000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(0L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(10));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(6));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(false));
    }

    @Test
    public void should_pass_valid_arguments_4_sleep_thread_time_adjusted() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "4", "11000");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(4));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(10000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(0L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(10));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(6));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(false));
    }

    @Test
    public void should_pass_valid_arguments_5_limiting_time_execution() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "4", "5", "10");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(4));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(5000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(10000L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(10));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(6));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(false));
    }

    @Test
    public void should_pass_valid_arguments_6_limiting_time_execution() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "4", "5", "10", "true");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(4));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(5000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(10000L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(10));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(6));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(true));
    }

    @Test
    public void should_pass_valid_arguments_7_invalid_toFile_defaults_false() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "3", "2", "9", "truessd");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(3));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(2000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(9000L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(10));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(6));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(false));
    }

    @Test
    public void should_pass_valid_arguments_8_setting_node_depth_and_site_height() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "4", "5", "10", "true", "99", "9");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(4));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(5000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(10000L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(99));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(9));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(true));
    }

    @Test
    public void should_pass_valid_arguments_9_limiting_node_depth_and_site_height() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "4", "5", "10", "true", "200", "20");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(4));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(5000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(10000L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(100));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(10));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(true));
    }

    @Test
    public void should_pass_valid_arguments_10_negative_node_depth_and_site_height() {
        CrawlerApp underTest = CrawlerApp.of("http://127.0.0.1:8000/", "4", "5", "10", "true", "-200", "-20");
        MatcherAssert.assertThat(underTest, Matchers.notNullValue());
        MatcherAssert.assertThat(underTest.getConfig().workerCount, Matchers.is(4));
        MatcherAssert.assertThat(underTest.getConfig().sleepTime, Matchers.is(5000L));
        MatcherAssert.assertThat(underTest.getConfig().timeout, Matchers.is(10000L));
        MatcherAssert.assertThat(underTest.getConfig().maxChildLinks, Matchers.is(0));
        MatcherAssert.assertThat(underTest.getConfig().siteHeight, Matchers.is(0));
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(true));
    }

    @Test
    public void should_have_defaults() {
        CrawlerApp underTest = new CrawlerApp(CrawlerAppConfigBuilder.newBuilder("http://127.0.0.1:8000/").build());
        DefaultCrawlerConfiguration expected = JsonHelper.getInstance().fromJsonFile("config.json",
                DefaultCrawlerConfiguration.class);

        Assertions.assertEquals(underTest.getConfig().workerCount, expected.workerCount);
        Assertions.assertEquals(underTest.getConfig().maxChildLinks, expected.maxSiteNodeLinks);
        Assertions.assertEquals(underTest.getConfig().siteHeight, expected.siteHeight);
        Assertions.assertEquals(underTest.getConfig().sleepTime, expected.sleepTime * 1000);
        Assertions.assertEquals(underTest.getConfig().timeout, expected.maxExecutionTime);
        MatcherAssert.assertThat(underTest.getConfig().siteURI, Matchers.is("http://127.0.0.1:8000"));
        MatcherAssert.assertThat(underTest.getConfig().reportToFile, Matchers.is(false));
    }
}