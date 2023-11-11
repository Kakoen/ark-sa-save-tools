package net.kakoen.arksa.savetools.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonUtils {

    public static void writeJsonToFile(Object object, Path file) throws IOException {
        ObjectWriter writer = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
                .writer(new DefaultPrettyPrinter());

        writeJsonToFile(object, file, writer);
    }

    public static void writeJsonToFile(Object object, Path file, ObjectWriter objectWriter) throws IOException {
        Files.createDirectories(file.getParent());
        objectWriter.writeValue(file.toFile(), object);
    }

}
