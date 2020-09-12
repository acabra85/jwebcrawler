package com.acabra.jwebcrawler.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlValidator {

    private UrlValidator() {}

    public static URL buildURL(String subDomainStr){
        if (null == subDomainStr || subDomainStr.trim().isEmpty()) {
            throw new NullPointerException("Sub-domain not found: expected sub-domain passed as argument.");
        }
        try {
            return new URL(subDomainStr);
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException("Sub-domain invalid: " + mue.getLocalizedMessage());
        }
    }
}
