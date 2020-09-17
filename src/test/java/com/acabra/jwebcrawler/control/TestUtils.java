package com.acabra.jwebcrawler.control;

import com.acabra.jwebcrawler.view.FileWriterAppender;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLSession;

public class TestUtils {

    static CompletableFuture<HttpResponse<String>> getFutureResponseOF(String fileName) {
        return CompletableFuture.completedFuture(successHTMLResponseOf(getResourceAsText(fileName)));
    }

    static CompletableFuture<HttpResponse<String>> getFutureEmptyResponse() {
        return CompletableFuture.completedFuture(notFoundHTMLResponse());
    }

    static CompletableFuture<HttpResponse<String>> getFutureRedirectResponse() {
        return CompletableFuture.completedFuture(redirectHTMLResponse());
    }

    public static CompletableFuture<HttpResponse<String>> getFutureRedirectResponseTo(String redirectTo) {
        return CompletableFuture.completedFuture(redirectHTMLResponseTo(redirectTo));
    }

    static HttpResponse<String> redirectHTMLResponse() {
        return buildHttpResponse("", "", 301, Map.of(
                "content-type", List.of("text/html"),
                "Location", List.of("/mypdffile.pdf")
        ));
    }

    static HttpResponse<String> redirectHTMLResponseTo(String redirectTo) {
        return buildHttpResponse("", "", 301, Map.of(
                "content-type", List.of("text/html"),
                "Location", List.of(redirectTo)
        ));
    }

    static CompletableFuture<HttpResponse<String>> getFuturePDFResponse() {
        return CompletableFuture.completedFuture(successPDFResponse());
    }

    static HttpResponse<String> successPDFResponse() {
        return buildHttpResponse("", "", 200, Map.of("content-type", List.of("application/pdf")));
    }

    static HttpResponse<String> notFoundHTMLResponse() {
        return buildHttpResponse("", "", 404, Map.of("content-type", List.of("text/html")));
    }

    static HttpResponse<String> successHTMLResponseOf(String body) {
        return buildHttpResponse("", body, 200, Map.of("content-type", List.of("text/html")));
    }

    static String getResourceAsText(String fileName) {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = CrawlerAppTest.class.getClassLoader().getResourceAsStream(fileName);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = null;
            while((line = br.readLine())!=null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        return "";
    }

    static HttpResponse<String> buildHttpResponse(String uri, String body, int statusCode, Map<String,
            List<String>> headers) {
        return new HttpResponse<>() {
            @Override public int statusCode() { return statusCode;}
            @Override public HttpRequest request() { return null;}
            @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty();}
            @Override public HttpHeaders headers() { return HttpHeaders.of(headers, (s, s2) -> true);}
            @Override public String body() { return body;}
            @Override public Optional<SSLSession> sslSession() { return Optional.empty();}
            @Override public URI uri() { return URI.create(uri);}
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_2;}
        };
    }

    public static File retrieveFileReportCreated(String expectedName) {
        long currentTime = System.currentTimeMillis();
        File currentFolder = new File(
                String.join(System.getProperty("file.separator"), System.getProperty("user.dir"),"reports"));
        if(!currentFolder.exists()) return null;
        for(File file: currentFolder.listFiles()) {
            if (file.isFile()
                    && file.getName().contains(expectedName)
                    && (currentTime - file.lastModified()) < 500L) {
                return file;
            }
        }
        return null;
    }

    public static String getTextFromFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = null;
            while((line = br.readLine())!= null ) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    public static boolean reportFileWasCreated(String siteUri) {
        String name = siteUri.contains("//") ? siteUri.split("//")[1] : siteUri;
        return retrieveFileReportCreated(FileWriterAppender.buildFileNameFromURI(name)) != null;
    }

    public static boolean cleanReportsFolder() {
        File directoryToDelete = new File(
                String.join(System.getProperty("file.separator"), System.getProperty("user.dir"),"reports"));
        if(!directoryToDelete.exists()) {
            return true;
        }
        for(File file: Objects.requireNonNull(directoryToDelete.listFiles())) {
            if(!file.delete()) {
                return false;
            }
        }
        return directoryToDelete.delete();
    }

    public static <T> CompletableFuture<HttpResponse<T>> getFutureResponse() {
        return null;
    }
}
