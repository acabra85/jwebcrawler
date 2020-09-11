package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.dto.DefaultCrawlerConfiguration;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.utils.JsonHelper;
import com.acabra.jwebcrawler.utils.UrlValidator;
import java.net.URL;
import java.util.Objects;

class CrawlerAppBuilder {

    private final static Integer DEFAULT_T_SLEEP_TIME = 1;
    private static final JsonHelper jsonHelper = JsonHelper.getInstance();
    final DefaultCrawlerConfiguration defaults;

    private String domain;
    private int workerCount;
    private double sleepTime;
    private double maxExecutionTime;
    private int siteHeight;
    private int maxSiteNodeLinks;
    private boolean reportToFile;

    CrawlerAppBuilder() {
        defaults = jsonHelper.fromJsonFile("config.json", DefaultCrawlerConfiguration.class);
        this.domain = null;
        this.workerCount = defaults.workerCount;
        this.sleepTime = defaults.sleepTime;
        this.maxExecutionTime = defaults.maxExecutionTime;
        this.siteHeight = defaults.siteHeight;
        this.maxSiteNodeLinks = defaults.maxSiteNodeLinks;
        this.reportToFile = defaults.reportToFile;
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

    public CrawlerAppBuilder withMaxExecutionTime(double maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
        return this;
    }

    public CrawlerAppBuilder withReportToFile(boolean reportToFile) {
        this.reportToFile = reportToFile;
        return this;
    }

    public CrawlerApp build() {
        URL rootUrl = UrlValidator.buildURL(domain);
        CrawlerAppConfig appConfig = new CrawlerAppConfig(
                rootUrl,
                this.workerCount,
                this.sleepTime,
                this.maxExecutionTime,
                this.siteHeight,
                this.maxSiteNodeLinks,
                this.reportToFile
        );
        return new CrawlerApp(appConfig);
    }


    private CrawlerAppBuilder of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime,
                                 String maxExecutionTime, String reportToFile) {
        return withDomain(subDomainStr)
                .withWorkerCount(Integer.parseInt(reqThreadCountStr))
                .withSleepWorkerTime(Double.parseDouble(reqSleepThreadTime))
                .withMaxExecutionTime(Double.parseDouble(maxExecutionTime))
                .withReportToFile(Boolean.parseBoolean(reportToFile));
    }

    private CrawlerAppBuilder of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime, String maxExecutionTime) {
        return of (subDomainStr, reqThreadCountStr, reqSleepThreadTime, maxExecutionTime, "");
    }

    private CrawlerAppBuilder of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime) {
        return of(subDomainStr, reqThreadCountStr, reqSleepThreadTime, "0");
    }

    private CrawlerAppBuilder of(String subDomainStr, String reqThreadCountStr) {
        return of(subDomainStr, reqThreadCountStr, DEFAULT_T_SLEEP_TIME.toString());
    }

    private CrawlerAppBuilder of(String subDomainStr) {
        return of(subDomainStr, String.valueOf(Integer.MAX_VALUE));
    }

    public CrawlerAppBuilder of(String... args) {
        if (args.length > 0) {
            if (args.length == 1) {
                return of(args[0]);
            } else if(args.length == 2) {
                return of(args[0], args[1]);
            }  else if(args.length == 3) {
                return of(args[0], args[1], args[2]);
            }
            return of(args[0], args[1], args[2], args[3]);
        }
        throw new NullPointerException("Sub-domain not found: This application expects at least 1 argument upon run.");
    }

}