package com.acabra.jwebcrawler.model;

import java.net.URL;

public class CrawlerAppConfig {


    private final static Long MIN_SLEEP_TIME = 100L;
    private final static Long MAX_SLEEP_TIME = 10000L;

    public final URL rootUrl;
    public final boolean reportToFile;
    public final int workerCount;
    public final long sleepTime;
    public final long timeout;
    public final int siteHeight;
    public final int maxChildLinks;
    public final boolean isStoppable;
    public final String startUri;
    public final String siteUri;

    public CrawlerAppConfig(URL rootUrl, int workerCount, double sleepTime, double maxExecutionTime,
                            int siteHeight, int maxSiteNodeLinks, boolean reportToFile) {
        long totalSleepTime = Double.valueOf(Math.min(sleepTime, 10.0) * 1000).longValue();
        this.rootUrl = rootUrl;
        this.workerCount = Math.max(workerCount, 1);
        this.sleepTime = Math.min(MAX_SLEEP_TIME, Math.max(totalSleepTime, MIN_SLEEP_TIME));
        this.timeout = Double.valueOf(maxExecutionTime * 1000).longValue();
        this.reportToFile = reportToFile;
        this.siteHeight = siteHeight;
        this.maxChildLinks = maxSiteNodeLinks;
        this.isStoppable = this.timeout > 0;
        this.siteUri = String.format("%s://%s", rootUrl.getProtocol(), rootUrl.getAuthority());
        this.startUri = siteUri + rootUrl.getPath();
    }
}
