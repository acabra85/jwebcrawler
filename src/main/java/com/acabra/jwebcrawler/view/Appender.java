package com.acabra.jwebcrawler.view;

import java.io.IOException;
import java.util.Optional;

public interface Appender {

    Appender append(String text) throws IOException;

    void close() throws IOException;

    Optional<String> textIfAvailable();
}
