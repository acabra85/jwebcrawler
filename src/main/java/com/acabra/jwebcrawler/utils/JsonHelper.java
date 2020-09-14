package com.acabra.jwebcrawler.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonHelper {

    private static final JsonHelper instance = new JsonHelper();
    private final Logger logger = LoggerFactory.getLogger(JsonHelper.class);

    private final ObjectMapper mapper;

    public JsonHelper(){
        mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
    }

    public static JsonHelper getInstance() {
        return instance;
    }

    public <T> T fromJsonFile(String fileName, Class<T> clazz) {
        try {
            return mapper.readValue(getFileContents(fileName), clazz);
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }
        return null;
    }

    private static String getFileContents(String fileName) throws IOException {
        InputStream resourceAsStream = Objects.requireNonNull(JsonHelper.class.getClassLoader().getResourceAsStream(fileName));
        BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));
        return br.readLine();
    }
}