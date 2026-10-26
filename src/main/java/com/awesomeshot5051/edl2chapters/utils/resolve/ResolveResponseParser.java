package com.awesomeshot5051.edl2chapters.utils.resolve;

import com.awesomeshot5051.edl2chapters.model.Marker;
import com.awesomeshot5051.edl2chapters.model.TimelineInfo;

import java.util.ArrayList;
import java.util.List;

public final class ResolveResponseParser {

    private ResolveResponseParser() {
    }

    /** Parses {@code <1|0>\t<name>} lines; timeline indexes are 1-based like Resolve's. */
    public static List<TimelineInfo> parseTimelines(String body) {
        List<TimelineInfo> timelines = new ArrayList<>();
        int index = 0;
        for (String line : body.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", 2);
            timelines.add(new TimelineInfo(++index, parts.length > 1 ? parts[1] : "", parts[0].equals("1")));
        }
        return timelines;
    }

    /** Parses {@code <seconds>\t<name>} lines. */
    public static List<Marker> parseMarkers(String body) {
        List<Marker> markers = new ArrayList<>();
        for (String line : body.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t", 2);
            markers.add(new Marker(Long.parseLong(parts[0].trim()), parts.length > 1 ? parts[1].trim() : ""));
        }
        return markers;
    }
}
