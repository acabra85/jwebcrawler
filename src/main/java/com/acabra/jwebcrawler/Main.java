package com.acabra.jwebcrawler;

import com.acabra.jwebcrawler.control.CrawlerApp;

public class Main {

    private Main(){}

    public static void main(String... args) {
        CrawlerApp.of(args).start();
    }
}
