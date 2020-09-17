package com.acabra.jwebcrawler.view;

import com.acabra.jwebcrawler.control.TestUtils;
import java.io.File;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileWriterAppenderTest {

    @BeforeEach
    public void clean() {
        TestUtils.cleanReportsFolder();
    }

    @Test
    public void test_file_name_escaping_1() {
        String filtered = FileWriterAppender.buildFileNameFromURI("%$%%$$#");
        MatcherAssert.assertThat(filtered, Matchers.is("_".repeat(filtered.length())));
    }

    @Test
    public void test_file_name_escaping_2() {
        String filtered = FileWriterAppender.buildFileNameFromURI("%$%%$$#he.l-5l_o");
        MatcherAssert.assertThat(filtered, Matchers.is("_______he.l-5l_o"));
    }

    @Test
    public void should_build_file_folder_does_not_exists() throws IOException {
        FileWriterAppender underTest = new FileWriterAppender("http://mysite.com", "001");
        underTest.append("somerandomtext");

        MatcherAssert.assertThat(underTest.textIfAvailable().isPresent(), Matchers.is(false));
        MatcherAssert.assertThat(new File("reports/CrawlReport-mysite.com-001.txt").exists(), Matchers.is(true));
    }
}