package com.acabra.jwebcrawler.utils;

import com.acabra.jwebcrawler.dto.DefaultCrawlerConfiguration;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonHelperTest {

    @Test
    void should_fail_non_existent_file() {
        DefaultCrawlerConfiguration expectedConfig = JsonHelper.getInstance()
                .fromJsonFile("non_existent_file", DefaultCrawlerConfiguration.class);

        MatcherAssert.assertThat(expectedConfig, Matchers.nullValue());
    }

    @Test
    void should_parse_config_file() {
        DefaultCrawlerConfiguration expectedConfig = JsonHelper.getInstance()
                .fromJsonFile("config.json", DefaultCrawlerConfiguration.class);

        MatcherAssert.assertThat(expectedConfig, Matchers.notNullValue());
        MatcherAssert.assertThat(expectedConfig.workerCount, Matchers.is(1));
        MatcherAssert.assertThat(expectedConfig.sleepTime, Matchers.is(1.0));
        MatcherAssert.assertThat(expectedConfig.maxExecutionTime, Matchers.is(30.0));
        MatcherAssert.assertThat(expectedConfig.siteHeight, Matchers.is(6));
        MatcherAssert.assertThat(expectedConfig.maxSiteNodeLinks, Matchers.is(10));
        MatcherAssert.assertThat(expectedConfig.reportToFile, Matchers.is(false));

    }
}