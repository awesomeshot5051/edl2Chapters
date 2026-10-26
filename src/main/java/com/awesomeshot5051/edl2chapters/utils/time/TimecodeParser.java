package com.awesomeshot5051.edl2chapters.utils.time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimecodeParser {

    private static final Pattern TIMECODE = Pattern.compile("^(\\d{1,2}):(\\d{2}):(\\d{2})[:;](\\d{2,3})$");
    private static final Pattern CLOCK = Pattern.compile("^(?:(\\d+):)?(\\d{1,2}):(\\d{2})$");

    private TimecodeParser() {
    }

    /** Parses {@code HH:MM:SS:FF} (or {@code ;} before frames) into whole seconds; frames are dropped. */
    public static long parseTimecode(String timecode) {
        Matcher matcher = TIMECODE.matcher(timecode.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid timecode: " + timecode);
        }
        return toSeconds(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    /** Parses {@code HH:MM:SS}, {@code MM:SS} or a plain number of seconds. */
    public static long parseClock(String clock) {
        String trimmed = clock.trim();
        if (trimmed.matches("\\d+")) {
            return Long.parseLong(trimmed);
        }
        Matcher matcher = CLOCK.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time: " + clock);
        }
        return toSeconds(matcher.group(1) == null ? "0" : matcher.group(1), matcher.group(2), matcher.group(3));
    }

    private static long toSeconds(String hours, String minutes, String seconds) {
        return Long.parseLong(hours) * 3600 + Long.parseLong(minutes) * 60 + Long.parseLong(seconds);
    }
}
