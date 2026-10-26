package com.awesomeshot5051.edl2chapters.app.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliMainTest {

    private static final String SAMPLE = "src/test/resources/Mission16Markers.edl";
    private static final String EXPECTED = "src/test/resources/expected_Mission16.txt";

    private static String trimmed(String text) {
        return Stream.of(text.split("\\R")).map(String::stripTrailing).collect(Collectors.joining("\n"));
    }

    @Test
    void goldenSampleMatchesExpected() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(0, CliMain.run(new String[]{SAMPLE}, new PrintStream(out), new PrintStream(new ByteArrayOutputStream())));
        assertEquals(trimmed(Files.readString(Paths.get(EXPECTED))), trimmed(out.toString()));
    }

    @Test
    void writesOutputFile(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("chapters.txt");
        assertEquals(0, CliMain.run(new String[]{SAMPLE, "-o", target.toString(), "--no-intro"},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())));
        assertEquals("00:00 Sneak Peak", trimmed(Files.readString(target)).lines().findFirst().orElseThrow());
    }

    @Test
    void missingFileFailsNonZero() {
        assertEquals(1, CliMain.run(new String[]{"does-not-exist.edl"},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())));
        assertEquals(1, CliMain.run(new String[]{"--bogus"},
                new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())));
    }
}
