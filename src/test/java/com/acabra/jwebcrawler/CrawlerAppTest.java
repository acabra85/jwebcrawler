package com.acabra.jwebcrawler;


import org.junit.Before;
import org.junit.Test;

public class CrawlerAppTest {

    private CrawlerApp underTest;

    @Before
    public void start() {
        underTest = new CrawlerApp();
    }

    @Test(expected = NullPointerException.class)
    public void should_fail_null_domain_given() {
        underTest.start(null);
    }

    @Test(expected = NullPointerException.class)
    public void should_fail_empty_spaces_as_domain_given() {
        underTest.start("     ");
    }

    @Test(expected = NullPointerException.class)
    public void should_fail_empty_domain_given() {
        underTest.start("");
    }
}