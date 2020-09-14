package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.CrawledNode;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import java.util.List;
import java.util.stream.IntStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CrawlProducerWorkerTest {

    private String getHtmlWithLinks() {
        return "<html lang=\"en\">\n" +
                "<body>\n" +
                "<a href=\"https://www.someexternalwebsite.com/\">some website</a>\n" +
                "<a href=\"/index.html\">index</a>\n" +
                "<div><a href=\"/a5.html\">a5</a></div>" +
                "<div><a href=\"/a7.html\">a7</a></div>" +
                "<div><a href=\"/a8.html\">a8</a></div>" +
                "<div><a href=\"/a8.html\">a8</a></div>" +
                "<div><a href=\"/a5.html\">a5</a></div>" +
                "</body>\n" +
                "</html>";
    }

    @Test
    public void should_return_unique_sub_domain_links() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder
                .newBuilder("http://mysite.com/")
                .withMaxSiteNodeLinks(0) //dont limit child nodes
                .build();
        CrawlProducerWorker underTest = new CrawlProducerWorker(
                new CrawlerCoordinator(), defaults, null, new CrawledNode(defaults.startUri, 0L), null);
        List<String> expected = List.of("http://mysite.com/index.html",
                "http://mysite.com/a5.html",
                "http://mysite.com/a7.html",
                "http://mysite.com/a8.html");
        List<String> actualLinks = underTest.extractLinks(getHtmlWithLinks(), "http://mysite.com");

        Assertions.assertEquals(actualLinks.size(), expected.size());
        IntStream.range(0, actualLinks.size()).forEach(i ->
                MatcherAssert.assertThat(actualLinks.get(i), Matchers.is(expected.get(i))));
    }

    @Test
    public void should_return_unique_sub_domain_links_limited() {
        CrawlerAppConfig defaults = CrawlerAppConfigBuilder
                .newBuilder("http://mysite.com/")
                .withMaxSiteNodeLinks(2)
                .build();
        CrawlProducerWorker underTest = new CrawlProducerWorker(
                new CrawlerCoordinator(), defaults, null, new CrawledNode(defaults.startUri, 0L), null);
        List<String> expected = List.of("http://mysite.com/index.html", "http://mysite.com/a5.html");
        List<String> actualLinks = underTest.extractLinks(getHtmlWithLinks(), "http://mysite.com");

        Assertions.assertEquals(actualLinks.size(), expected.size());
        IntStream.range(0, actualLinks.size()).forEach(i ->
                MatcherAssert.assertThat(actualLinks.get(i), Matchers.is(expected.get(i))));
    }

}