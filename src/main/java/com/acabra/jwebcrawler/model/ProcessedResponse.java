package com.acabra.jwebcrawler.model;

import java.util.List;

public class ProcessedResponse {
    public final List<String> links;
    public final int statusCode;
    public final boolean success;

    public ProcessedResponse(int statusCode, List<String> links, boolean success) {
        this.statusCode = statusCode;
        this.links = links;
        this.success = success;
    }

    public static ProcessedResponse ofError(String url) {
        return new ProcessedResponse(0, List.of(url), false);
    }
}