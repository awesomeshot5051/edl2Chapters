package com.awesomeshot5051.edl2chapters.utils.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileReaderUtil {

    private static final char BOM = '﻿';

    private FileReaderUtil() {
    }

    public static String readText(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return !text.isEmpty() && text.charAt(0) == BOM ? text.substring(1) : text;
    }
}
