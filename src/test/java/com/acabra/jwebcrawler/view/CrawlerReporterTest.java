package com.acabra.jwebcrawler.view;

import com.acabra.jwebcrawler.control.TestUtils;
import com.acabra.jwebcrawler.model.CrawlSiteResponse;
import com.acabra.jwebcrawler.model.CrawledNode;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CrawlerReporterTest {


    private static final String SUB_DOMAIN = "example.com:80";
    private static final Comparator<CrawledNode> COMPARATOR = Comparator.comparing(a -> a.url);
    private static Appender stringAppender;
    private CrawlerReporter underTest;

    @BeforeEach
    public void setup() {
        TestUtils.cleanReportsFolder();
        underTest = new CrawlerReporter();
        stringAppender = new Appender() {

            StringBuilder sb = new StringBuilder();

            @Override
            public Appender append(String text) throws IOException {
                sb.append(text);
                return this;
            }

            @Override
            public void close() {
                //void
            }

            @Override
            public Optional<String> textIfAvailable() {
                return Optional.of(sb.toString());
            }
        };
    }

    @Test
    public void should_build_empty_report() {
        String actualReport = underTest
                .buildReport(new CrawlSiteResponse(SUB_DOMAIN, Collections.emptyMap(), 0, 0, 0L, 0.5, 0), stringAppender)
                .get();
        MatcherAssert.assertThat(actualReport, Matchers.is(CrawlerReporter.EMPTY_REPORT_CONTENT));
    }

    @Test
    public void should_build_non_empty_report_to_string() {
        Map<Long, PriorityQueue<CrawledNode>> graph = buildGraph();
        String actualReport = underTest
                .buildReport(new CrawlSiteResponse(SUB_DOMAIN, graph, 0, 0, 0L, 0.5, 0), stringAppender)
                .get();
        Assertions.assertTrue(actualReport.contains(CrawlerReporter.SITE_MAP_HEADER));
        Assertions.assertTrue(actualReport.contains(SUB_DOMAIN));
        Assertions.assertTrue(actualReport.contains("page.html"));
    }

    @Test
    public void should_build_non_empty_report_to_console() {
        Map<Long, PriorityQueue<CrawledNode>> graph = buildGraph();
        ConsoleAppender appender = new ConsoleAppender();
        Optional<String> textReport = underTest
                .buildReport(new CrawlSiteResponse(SUB_DOMAIN, graph, 0, 0, 0L, 0.5, 0), appender);

        Assertions.assertTrue(appender.textIfAvailable().isEmpty());
        Assertions.assertTrue(textReport.isEmpty());
    }

    @Test
    public void should_build_non_empty_report_to_file() throws IOException {
        Map<Long, PriorityQueue<CrawledNode>> graph = buildGraph();
        FileWriterAppender appender = new FileWriterAppender(SUB_DOMAIN, "identifier_001");
        Optional<String> reportResult = underTest
                .buildReport(new CrawlSiteResponse(SUB_DOMAIN, graph, 0, 0, 0L, 0.5, 8), appender);


        File reportFile = TestUtils.retrieveFileReportCreated(appender.getFileName());

        Assertions.assertTrue(reportResult.isEmpty());
        Assertions.assertNotNull(reportFile);

        String resultText = TestUtils.getTextFromFile(reportFile);

        Assertions.assertTrue(resultText.contains(CrawlerReporter.SITE_MAP_HEADER));
        Assertions.assertTrue(resultText.contains(SUB_DOMAIN));
        Assertions.assertTrue(resultText.contains("page.html"));
        Assertions.assertTrue(resultText.contains("Concurrent Workers: 8"));
    }

    @Test
    public void should_log_and_contain_exceptions() throws IOException {
        Appender appenderMock = Mockito.mock(Appender.class);
        Mockito.doThrow(IOException.class)
                .when(appenderMock).close();
        Mockito.doThrow(IOException.class)
                .when(appenderMock).append(Mockito.anyString());

        CrawlSiteResponse crawlResponse = new CrawlSiteResponse(SUB_DOMAIN, buildGraph(), 0, 0, 0L, 0.5, 0);
        Optional<String> report = underTest.buildReport(crawlResponse, appenderMock);

        MatcherAssert.assertThat(report.isPresent(), Matchers.is(true));
        MatcherAssert.assertThat(report.get(), Matchers.stringContainsInOrder("Unable to generate response"));
    }

    @Test
    public void should_build_report_file() {
        CrawlSiteResponse response = new CrawlSiteResponse("buildreportdomain.com", buildGraph(), 0, 0, 0L, 0.7, 0);

        underTest.report(true, response, "identifier_001");

        MatcherAssert.assertThat(TestUtils.reportFileWasCreated("buildreportdomain.com"), Matchers.is(true));
    }

    @Test
    public void should_no_build_report_file() {
        String site = "dontbuildreportdomain.com";
        CrawlSiteResponse response = new CrawlSiteResponse(site, buildGraph(), 0, 0, 0L, 0.7, 0);

        underTest.report(false, response, "useless_identifier");

        MatcherAssert.assertThat(TestUtils.reportFileWasCreated(site), Matchers.is(false));
    }

    @Test
    public void should_catch_and_log_exception() {
        CrawlSiteResponse response = new CrawlSiteResponse(null, null, 0, 8, 0L, 0.6, 0);

        underTest.report(true, response, "my_identifier");

        MatcherAssert.assertThat(TestUtils.reportFileWasCreated("yourdomain.com"), Matchers.is(false));
    }

    private Map<Long, PriorityQueue<CrawledNode>> buildGraph() {
        Map<Long, PriorityQueue<CrawledNode>> graph = new HashMap<>();
        CrawledNode rootNode = new CrawledNode(SUB_DOMAIN, 0L);
        CrawledNode childRoot = new CrawledNode(SUB_DOMAIN + "/page.html", 1L, rootNode.level + 1, rootNode.id);
        graph.put(CrawledNode.ROOT_NODE_PARENT_ID, new PriorityQueue<>(COMPARATOR) {{
            offer(rootNode);
        }});
        graph.put(rootNode.id, new PriorityQueue<>(COMPARATOR) {{
            offer(childRoot);
        }});
        return graph;
    }
}