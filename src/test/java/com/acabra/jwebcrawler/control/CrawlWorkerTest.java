package com.acabra.jwebcrawler.control;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class CrawlWorkerTest {

    final static String baseUri = "http://mysite.com";

    @Test
    public void should_return_unique_subdomain_links() {
        CrawlWorker underTest = CrawlWorker.of(null, null, baseUri, 0L, null, false, 10, 2, null);
        Set<String> expected = Set.of("http://mysite.com/index.html", "http://mysite.com/a5.html",
                "http://mysite.com/a7.html", "http://mysite.com/a8.html");
        List<String> actualLinks = underTest.extractLinks(getHtmlWithLinks(), baseUri);
        Assertions.assertEquals(actualLinks.size(), expected.size());
        actualLinks.forEach(link -> Assertions.assertTrue(expected.contains(link), link));
    }

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
}