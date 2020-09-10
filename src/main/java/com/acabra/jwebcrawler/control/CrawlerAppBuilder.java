package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.model.DefaultCrawlerConfiguration;
import com.acabra.jwebcrawler.service.DownloadService;
import com.acabra.jwebcrawler.utils.JsonHelper;

class CrawlerAppBuilder {

    private static final JsonHelper jsonHelper = JsonHelper.getInstance();
    final DefaultCrawlerConfiguration defaults;

    private String domain;
    private double sleepTime;
    private int workerCount;
    private int siteHeight;
    private int maxSiteNodeLinks;
    private double maxExecutionTime;

    CrawlerAppBuilder () {
        defaults = jsonHelper.fromJsonFile("config.json", DefaultCrawlerConfiguration.class);
        this.sleepTime = defaults.sleepTime;
        this.workerCount = defaults.workerCount;
        this.siteHeight = defaults.siteHeight;
        this.maxSiteNodeLinks = defaults.maxSiteNodeLinks;
        this.maxExecutionTime = defaults.maxExecutionTime;
    }

    public CrawlerAppBuilder withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public CrawlerAppBuilder withSleepWorkerTime(double sleepTime) {
        this.sleepTime = sleepTime;
        return this;
    }

    public CrawlerAppBuilder withWorkerCount(int workerCount) {
        this.workerCount = workerCount;
        return this;
    }

    public CrawlerAppBuilder withMaxTreeSiteHeight(int siteHeight) {
        this.siteHeight = siteHeight;
        return this;
    }

    public CrawlerAppBuilder withMaxSiteNodeLinks(int maxSiteNodeLinks) {
        this.maxSiteNodeLinks = maxSiteNodeLinks;
        return this;
    }

    public CrawlerAppBuilder withMaxExecutionTime(int maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
        return this;
    }

    public CrawlerApp build() {
        return CrawlerApp.of(this.domain, this.workerCount, this.sleepTime, this.siteHeight, this.maxSiteNodeLinks,
                this.maxExecutionTime);
    }
}