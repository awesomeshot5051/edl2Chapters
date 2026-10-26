package com.awesomeshot5051.edl2chapters.utils.time;

public final class ChapterTimeFormatter {

    private ChapterTimeFormatter() {
    }

    /** Formats seconds as {@code MM:SS}, or {@code H:MM:SS} from one hour up. */
    public static String format(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}
