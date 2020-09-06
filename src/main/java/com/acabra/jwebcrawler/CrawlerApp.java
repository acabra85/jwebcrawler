package com.acabra.jwebcrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlerApp {

    private final Logger logger = LoggerFactory.getLogger(CrawlerApp.class);

    public CrawlerApp() {
    }

    void start(String subDomain) {
        if (null != subDomain && !subDomain.trim().isEmpty()) {
            logger.info(String.format("Will attempt crawl for pages of SubDomain :<%s>", subDomain));
        } else {
            throw new NullPointerException("Sub-domain not found: expected sub-domain passed as argument.");
        }
    }

    public static void main(String... args) {
        if (args != null && args.length > 0) {
            new CrawlerApp().start(args[0]);
        } else {
            throw new NullPointerException("Sub-domain not found: expected sub-domain passed as argument.");
        }
    }
}
