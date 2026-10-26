package com.awesomeshot5051.edl2chapters.utils.chapters;

import com.awesomeshot5051.edl2chapters.model.ChapterOptions;
import com.awesomeshot5051.edl2chapters.model.Marker;
import com.awesomeshot5051.edl2chapters.utils.time.ChapterTimeFormatter;

import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

public final class ChapterBuilder {

    private ChapterBuilder() {
    }

    /** Applies the timeline offset, sorts by time and renders one chapter per line. */
    public static String build(List<Marker> markers, ChapterOptions options) {
        StringJoiner output = new StringJoiner("\n");
        if (options.includeIntro()) {
            output.add(ChapterTimeFormatter.format(0) + " " + options.introTitle());
        }
        markers.stream()
                .sorted(Comparator.comparingLong(Marker::seconds))
                .forEach(marker -> {
                    long shifted = marker.seconds() - options.offsetSeconds();
                    if (shifted < 0) {
                        throw new IllegalArgumentException("Offset is later than marker \"" + marker.name() + "\"");
                    }
                    output.add(ChapterTimeFormatter.format(shifted) + " " + marker.name());
                });
        return output.toString();
    }
}
