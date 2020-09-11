package com.acabra.jwebcrawler.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Objects;

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
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
        }
        return null;
    }

    private static String getFileContents(String fileName) throws IOException {
        InputStream resourceAsStream = Objects.requireNonNull(JsonHelper.class.getClassLoader().getResourceAsStream(fileName));
        BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));
        return br.readLine();
    }
}