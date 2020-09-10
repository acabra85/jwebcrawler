package com.acabra.jwebcrawler;

import com.acabra.jwebcrawler.control.CrawlerApp;
import com.acabra.jwebcrawler.service.DownloadService;

public class Main {
    public static void main(String... args) {
        DownloadService downloadService = DownloadService.getInstance();
        if (args != null && args.length > 0) {
            if (args.length == 1) {
                CrawlerApp.of(args[0]).start(downloadService);
            } else if(args.length == 2) {
                CrawlerApp.of(args[0], args[1]).start(downloadService);
            } else if(args.length == 3) {
                CrawlerApp.of(args[0], args[1], args[2]).start(downloadService);
            } else if(args.length == 4) {
                CrawlerApp.of(args[0], args[1], args[2], args[3]).start(downloadService);
            }
        } else {
            throw new NullPointerException("Sub-domain not found: This application expects at least 1 argument upon run.");
        }
    }
}
