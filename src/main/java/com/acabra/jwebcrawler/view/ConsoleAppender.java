package com.acabra.jwebcrawler.view;

import java.io.IOException;
import java.util.Optional;

public class ConsoleAppender implements Appender {

    @Override
    public Appender append(String text) throws IOException {
        System.out.print(text);
        return this;
    }

    @Override
    public void close() {
        System.out.flush();
    }

    @Override
    public Optional<String> textIfAvailable() {
        return Optional.empty();
    }
}
