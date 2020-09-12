package com.acabra.jwebcrawler.model;

import java.net.URL;

public class CrawlerAppConfig {

    private final static Long MIN_SLEEP_TIME = 100L;
    private final static Long MAX_SLEEP_TIME = 10000L;
    private final static int MAX_WORKER_COUNT = 50;
    private static final int MAX_CHILD_PER_PAGE = 10;
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

    public CrawlerAppConfig(URL rootUrl, int workerCount, double sleepTime, double maxExecutionTime,
                            int siteHeight, int maxSiteNodeLinks, boolean reportToFile) {
        long totalSleepTime = Double.valueOf(Math.min(sleepTime, 10.0) * 1000).longValue();
        this.rootUrl = rootUrl;
        this.siteURI = String.format("%s://%s", rootUrl.getProtocol(), rootUrl.getAuthority());
        this.startUri = siteURI + rootUrl.getPath();
        this.workerCount = Math.min(MAX_WORKER_COUNT, Math.max(workerCount, 1));
        this.sleepTime = Math.min(MAX_SLEEP_TIME, Math.max(totalSleepTime, MIN_SLEEP_TIME));
        this.timeout = Double.valueOf(maxExecutionTime * 1000).longValue();
        this.maxChildLinks = maxSiteNodeLinks <= 0 ? 0 : Math.min(MAX_CHILD_PER_PAGE, maxSiteNodeLinks);
        this.siteHeight = siteHeight <= 0 ? 0 : Math.min(MAX_DEPTH, siteHeight);
        this.reportToFile = reportToFile;
        this.isStoppable = this.timeout > 0;
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
}
