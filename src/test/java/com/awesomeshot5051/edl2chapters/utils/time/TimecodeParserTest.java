package com.awesomeshot5051.edl2chapters.utils.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimecodeParserTest {

    @Test
    void dropsFrames() {
        assertEquals(38, TimecodeParser.parseTimecode("00:00:38:21"));
        assertEquals(3723, TimecodeParser.parseTimecode("01:02:03:00"));
        assertEquals(3723, TimecodeParser.parseTimecode("01:02:03;29"));
    }

    @Test
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> TimecodeParser.parseTimecode("nope"));
    }

    @Test
    void parsesClock() {
        assertEquals(3600, TimecodeParser.parseClock("01:00:00"));
        assertEquals(125, TimecodeParser.parseClock("02:05"));
        assertEquals(90, TimecodeParser.parseClock("90"));
        assertThrows(IllegalArgumentException.class, () -> TimecodeParser.parseClock("x"));
    }
}
