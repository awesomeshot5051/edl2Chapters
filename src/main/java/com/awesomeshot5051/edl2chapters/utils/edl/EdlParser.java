package com.awesomeshot5051.edl2chapters.utils.edl;

import com.awesomeshot5051.edl2chapters.model.Marker;
import com.awesomeshot5051.edl2chapters.utils.time.TimecodeParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EdlParser {

    private static final String TC = "\\d{1,2}:\\d{2}:\\d{2}[:;]\\d{2,3}";
    private static final Pattern EVENT = Pattern.compile(
            "^\\s*\\d+\\s+\\S+\\s+\\S+\\s+\\S+\\s+(?:\\S+\\s+)?" + TC + "\\s+" + TC + "\\s+(" + TC + ")\\s+" + TC);
    private static final Pattern MARKER_NAME = Pattern.compile("\\|M:(.*?)\\s*(?:\\|D:|$)");

    private EdlParser() {
    }

    /** Extracts every Resolve marker (event line plus its {@code |M:} comment line) in file order. */
    public static List<Marker> parse(String edlText) {
        List<Marker> markers = new ArrayList<>();
        String[] lines = edlText.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            Matcher event = EVENT.matcher(lines[i]);
            if (!event.find()) {
                continue;
            }
            Matcher name = i + 1 < lines.length ? MARKER_NAME.matcher(lines[i + 1]) : null;
            if (name != null && name.find()) {
                markers.add(new Marker(TimecodeParser.parseTimecode(event.group(1)), name.group(1).trim()));
            }
        }
        return markers;
    }
}
