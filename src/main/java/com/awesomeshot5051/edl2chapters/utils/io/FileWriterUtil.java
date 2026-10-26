package com.awesomeshot5051.edl2chapters.utils.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileWriterUtil {

    private FileWriterUtil() {
    }

    public static void writeText(Path path, String text) throws IOException {
        Files.writeString(path, text + System.lineSeparator(), StandardCharsets.UTF_8);
    }
}
