package net.kakoen.arksa.savetools.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonUtils {

    private final static ObjectWriter DEFAULT_OBJECT_WRITER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
            .writer(new DefaultPrettyPrinter());

    public static void writeJsonToFile(Object object, Path file) throws IOException {
        writeJsonToFile(object, file, DEFAULT_OBJECT_WRITER);
    }

    public static void writeJsonToFile(Object object, Path file, ObjectWriter objectWriter) throws IOException {
        Files.createDirectories(file.getParent());
        objectWriter.writeValue(file.toFile(), object);
    }

    public static String toJsonString(Object object) {
        return toJsonString(object, DEFAULT_OBJECT_WRITER);
    }

    public static String toJsonString(Object object, ObjectWriter objectWriter) {
        try {
            return objectWriter.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
