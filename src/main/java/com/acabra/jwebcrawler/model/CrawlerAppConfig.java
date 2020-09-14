package com.acabra.jwebcrawler.model;

import java.net.URL;

public class CrawlerAppConfig {

    private final static Long MIN_SLEEP_TIME = 100L;
    private final static Long MAX_SLEEP_TIME = 10000L;
    private final static int MAX_WORKER_COUNT = 50;
    private static final int MAX_CHILD_PER_PAGE = 100;
    private static final int MAX_DEPTH = 10;

    public final URL rootUrl;
    public final boolean reportToFile;
    public final int workerCount;
    public final long sleepTime;
    public final long timeout;
    public final int siteHeight;
    public final int maxChildLinks;
    public final boolean isStoppable;
    public final String startUri;
    public final String siteURI;

    private CrawlerAppConfig(URL rootUrl, String siteURI, int workerCount, long sleepTime,
                             long timeout, int siteHeight, int maxSiteNodeLinks,
                             boolean reportToFile) {
        this.rootUrl = rootUrl;
        this.siteURI = siteURI;
        this.startUri = siteURI + rootUrl.getPath();
        this.workerCount = workerCount;
        this.sleepTime = sleepTime;
        this.timeout = timeout;
        this.siteHeight = siteHeight;
        this.maxChildLinks = maxSiteNodeLinks;
        this.reportToFile = reportToFile;
        this.isStoppable = this.timeout > 0;
    }

    public static CrawlerAppConfig of(URL rootUrl, int workerCount, double sleepTime, double maxExecutionTime,
                                      int siteHeight, int maxSiteNodeLinks, boolean reportToFile) {
        long totalSleepTime = Double.valueOf(Math.min(sleepTime, 10.0) * 1000).longValue();
        return new CrawlerAppConfig(
                rootUrl,
                String.format("%s://%s", rootUrl.getProtocol(), rootUrl.getAuthority()),
                Math.min(MAX_WORKER_COUNT, Math.max(workerCount, 1)),
                Math.min(MAX_SLEEP_TIME, Math.max(totalSleepTime, MIN_SLEEP_TIME)),
                Double.valueOf(maxExecutionTime * 1000).longValue(),
                siteHeight <= 0 ? 0 : Math.min(siteHeight, MAX_DEPTH),
                maxSiteNodeLinks <= 0 ? 0 : Math.min(maxSiteNodeLinks, MAX_CHILD_PER_PAGE),
                reportToFile);
    }

    @Override
    public String toString() {
        return "CrawlerAppConfig{" +
                "rootUrl=" + rootUrl +
                ", reportToFile=" + reportToFile +
                ", workerCount=" + workerCount +
                ", sleepTime=" + sleepTime +
                ", timeout=" + timeout +
                ", siteHeight=" + siteHeight +
                ", maxChildLinks=" + maxChildLinks +
                ", isStoppable=" + isStoppable +
                ", startUri='" + startUri + '\'' +
                ", siteURI='" + siteURI + '\'' +
                '}';
    }

    public CrawlerAppConfig copy() {
        return new CrawlerAppConfig(
                this.rootUrl,
                this.siteURI,
                this.workerCount,
                this.sleepTime,
                this.timeout,
                this.siteHeight,
                this.maxChildLinks,
                this.reportToFile
        );
    }
}
