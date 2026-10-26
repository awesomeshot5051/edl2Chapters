package com.awesomeshot5051.edl2chapters.utils.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChapterTimeFormatterTest {

    @Test
    void formatsMinutesAndHours() {
        assertEquals("00:00", ChapterTimeFormatter.format(0));
        assertEquals("00:38", ChapterTimeFormatter.format(38));
        assertEquals("59:59", ChapterTimeFormatter.format(3599));
        assertEquals("1:02:03", ChapterTimeFormatter.format(3723));
    }
}
