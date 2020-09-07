package com.acabra.jwebcrawler.utils;

import java.net.URL;

import com.acabra.jwebcrawler.utils.UrlValidator;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UrlValidatorTest {

    @Test
    public void should_pass_valid_domain_given_1() {
        URL url = UrlValidator.buildURL("https://mydomain.co.uk/");
        MatcherAssert.assertThat(url.getAuthority(), Matchers.is("mydomain.co.uk"));
    }

    @Test
    public void should_pass_valid_domain_given_2() {
        URL url = UrlValidator.buildURL("http://mydomain.co.uk/");
        MatcherAssert.assertThat(url.getAuthority(), Matchers.is("mydomain.co.uk"));
    }

    @Test
    public void should_pass_valid_domain_given_3() {
        URL url = UrlValidator.buildURL("http://another-9domain.com/");
        MatcherAssert.assertThat(url.getAuthority(), Matchers.is("another-9domain.com"));
    }

    @Test
    public void should_pass_valid_domain_given_domain_ip() {
        URL url = UrlValidator.buildURL("http://127.0.0.1/");
        MatcherAssert.assertThat(url.getAuthority(), Matchers.is("127.0.0.1"));
    }

    @Test
    public void should_fail_url_missing_protocol() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> UrlValidator.buildURL("mydomain.com"));
        Assertions.assertTrue(ex.getMessage().contains("Sub-domain invalid"));
    }

    @Test
    public void should_fail_url_invalid_protocol() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> UrlValidator.buildURL("httpx://mydomain.com"));
        Assertions.assertTrue(ex.getMessage().contains("Sub-domain invalid"));
    }

}