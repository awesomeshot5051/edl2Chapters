package com.awesomeshot5051.edl2chapters.app.cli;

import com.awesomeshot5051.edl2chapters.model.ChapterOptions;
import com.awesomeshot5051.edl2chapters.utils.chapters.ChapterBuilder;
import com.awesomeshot5051.edl2chapters.utils.edl.EdlParser;
import com.awesomeshot5051.edl2chapters.utils.io.FileReaderUtil;
import com.awesomeshot5051.edl2chapters.utils.io.FileWriterUtil;
import com.awesomeshot5051.edl2chapters.utils.time.TimecodeParser;

import java.io.PrintStream;
import java.nio.file.Path;

public final class CliMain {

    public static final String USAGE = """
            Usage: edl2chapters <input.edl> [-o <output.txt>] [--offset <HH:MM:SS>] [--intro-title <text>] [--no-intro]
              (run with no arguments to open the GUI)""";

    private CliMain() {
    }

    /** Runs the CLI and returns the process exit code. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Path input = null;
            Path output = null;
            long offset = 0;
            boolean includeIntro = true;
            String introTitle = ChapterOptions.DEFAULT_INTRO_TITLE;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-o", "--output" -> output = Path.of(requireValue(args, ++i, args[i - 1]));
                    case "--offset" -> offset = TimecodeParser.parseClock(requireValue(args, ++i, args[i - 1]));
                    case "--intro-title" -> introTitle = requireValue(args, ++i, args[i - 1]);
                    case "--no-intro" -> includeIntro = false;
                    case "-h", "--help" -> {
                        out.println(USAGE);
                        return 0;
                    }
                    default -> {
                        if (args[i].startsWith("-") || input != null) {
                            throw new IllegalArgumentException("Unexpected argument: " + args[i]);
                        }
                        input = Path.of(args[i]);
                    }
                }
            }
            if (input == null) {
                throw new IllegalArgumentException("No input file given");
            }
            String chapters = ChapterBuilder.build(
                    EdlParser.parse(FileReaderUtil.readText(input)),
                    new ChapterOptions(offset, includeIntro, introTitle));
            if (output == null) {
                out.println(chapters);
            } else {
                FileWriterUtil.writeText(output, chapters);
            }
            return 0;
        } catch (Exception e) {
            err.println("Error: " + e.getMessage());
            err.println(USAGE);
            return 1;
        }
    }

    private static String requireValue(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + flag);
        }
        return args[index];
    }
}
