package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.dto.DefaultCrawlerConfiguration;
import com.acabra.jwebcrawler.model.CrawlerAppConfig;
import com.acabra.jwebcrawler.utils.JsonHelper;
import com.acabra.jwebcrawler.utils.UrlValidator;
import java.net.URL;

class CrawlerAppConfigBuilder {

    private static final JsonHelper jsonHelper = JsonHelper.getInstance();
    final DefaultCrawlerConfiguration defaults;

    private String domain;
    private int workerCount;
    private double sleepTime;
    private double maxExecutionTime;
    private int siteHeight;
    private int maxSiteNodeLinks;
    private boolean reportToFile;

    CrawlerAppConfigBuilder() {
        defaults = jsonHelper.fromJsonFile("config.json", DefaultCrawlerConfiguration.class);
        this.domain = null;
        this.workerCount = defaults.workerCount;
        this.sleepTime = defaults.sleepTime;
        this.maxExecutionTime = defaults.maxExecutionTime;
        this.siteHeight = defaults.siteHeight;
        this.maxSiteNodeLinks = defaults.maxSiteNodeLinks;
        this.reportToFile = defaults.reportToFile;
    }
    public CrawlerAppConfigBuilder withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public CrawlerAppConfigBuilder withSleepWorkerTime(double sleepTime) {
        this.sleepTime = sleepTime;
        return this;
    }

    public CrawlerAppConfigBuilder withWorkerCount(int workerCount) {
        this.workerCount = workerCount;
        return this;
    }

    public CrawlerAppConfigBuilder withMaxTreeSiteHeight(int siteHeight) {
        this.siteHeight = siteHeight;
        return this;
    }

    public CrawlerAppConfigBuilder withMaxSiteNodeLinks(int maxSiteNodeLinks) {
        this.maxSiteNodeLinks = maxSiteNodeLinks;
        return this;
    }

    public CrawlerAppConfigBuilder withMaxExecutionTime(double maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
        return this;
    }

    public CrawlerAppConfigBuilder withReportToFile(boolean reportToFile) {
        this.reportToFile = reportToFile;
        return this;
    }

    public CrawlerAppConfig build() {
        URL rootUrl = UrlValidator.buildURL(domain);
        return new CrawlerAppConfig(
                rootUrl,
                this.workerCount,
                this.sleepTime,
                this.maxExecutionTime,
                this.siteHeight,
                this.maxSiteNodeLinks,
                this.reportToFile
        );
    }

    private CrawlerAppConfigBuilder of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime,
                                       String maxExecutionTime, String reportToFile, String maxChildren,
                                       String maxSiteHeight) {
        return of(subDomainStr, reqThreadCountStr, reqSleepThreadTime, maxExecutionTime, reportToFile, maxChildren)
                .withMaxTreeSiteHeight(Integer.parseInt(maxSiteHeight));
    }

    private CrawlerAppConfigBuilder of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime,
                                       String maxExecutionTime, String reportToFile, String maxSiteNodeLinks) {
        return of(subDomainStr, reqThreadCountStr, reqSleepThreadTime, maxExecutionTime, reportToFile)
                .withMaxSiteNodeLinks(Integer.parseInt(maxSiteNodeLinks));
    }


    private CrawlerAppConfigBuilder of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime,
                                       String maxExecutionTime, String reportToFile) {
        return of(subDomainStr, reqThreadCountStr, reqSleepThreadTime, maxExecutionTime)
                .withReportToFile(Boolean.parseBoolean(reportToFile));
    }

    private CrawlerAppConfigBuilder of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime, String maxExecutionTime) {
        return of(subDomainStr, reqThreadCountStr, reqSleepThreadTime)
                .withMaxExecutionTime(Double.parseDouble(maxExecutionTime));
    }

    private CrawlerAppConfigBuilder of(String subDomainStr, String reqThreadCountStr, String reqSleepThreadTime) {
        return of(subDomainStr, reqThreadCountStr)
                .withSleepWorkerTime(Double.parseDouble(reqSleepThreadTime));
    }

    private CrawlerAppConfigBuilder of(String subDomainStr, String reqThreadCountStr) {
        return of(subDomainStr)
                .withWorkerCount(Integer.parseInt(reqThreadCountStr));
    }

    private CrawlerAppConfigBuilder of(String subDomainStr) {
        return this.withDomain(subDomainStr);
    }


    public static CrawlerAppConfigBuilder newBuilder(String... args) {
        CrawlerAppConfigBuilder builder = new CrawlerAppConfigBuilder();
        if (args.length > 0) {
            if (args.length == 1) {
                return builder.of(args[0]);
            } else if(args.length == 2) {
                return builder.of(args[0], args[1]);
            }  else if(args.length == 3) {
                return builder.of(args[0], args[1], args[2]);
            } else if (args.length == 4) {
                return builder.of(args[0], args[1], args[2], args[3]);
            } else if (args.length == 5) {
                return builder.of(args[0], args[1], args[2], args[3], args[4]);
            } else if (args.length == 6) {
                return builder.of(args[0], args[1], args[2], args[3], args[4], args[5]);
            }
            return builder.of(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
        }
        throw new NullPointerException("Sub-domain not found: This application expects at least 1 argument upon run.");
    }

}