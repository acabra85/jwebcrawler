package com.acabra.jwebcrawler.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultCrawlerConfiguration {
    public final double sleepTime;
    public final int workerCount;
    public final int siteHeight;
    public final int maxSiteNodeLinks;
    public final double maxExecutionTime;
    public final boolean reportToFile;

    @JsonCreator
    public DefaultCrawlerConfiguration(@JsonProperty(value = "sleepTime", required = true)
                                       int sleepTime,
                                       @JsonProperty(value = "workerCount", required = true)
                                       int workerCount,
                                       @JsonProperty(value = "siteHeight", required = true)
                                       int siteHeight,
                                       @JsonProperty(value = "maxSiteNodeLinks", required = true)
                                       int maxSiteNodeLinks,
                                       @JsonProperty(value = "maxExecutionTime", required = true)
                                       int maxExecutionTime,
                                       @JsonProperty(value = "reportToFile", required = true)
                                       boolean reportToFile
                                       ) {
        this.sleepTime = sleepTime;
        this.workerCount = workerCount;
        this.siteHeight = siteHeight;
        this.maxSiteNodeLinks = maxSiteNodeLinks;
        this.maxExecutionTime = maxExecutionTime;
        this.reportToFile = reportToFile;
    }
}
