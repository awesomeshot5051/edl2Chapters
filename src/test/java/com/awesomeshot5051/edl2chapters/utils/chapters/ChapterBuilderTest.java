package com.awesomeshot5051.edl2chapters.utils.chapters;

import com.awesomeshot5051.edl2chapters.model.ChapterOptions;
import com.awesomeshot5051.edl2chapters.model.Marker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChapterBuilderTest {

    private static final List<Marker> MARKERS = List.of(new Marker(3700, "B"), new Marker(3605, "A"));

    @Test
    void sortsAndAppliesOffset() {
        assertEquals("00:05 A\n01:40 B", ChapterBuilder.build(MARKERS, new ChapterOptions(3600, false, "")));
    }

    @Test
    void introOptions() {
        assertEquals("00:00 Start\n1:00:05 A\n1:01:40 B",
                ChapterBuilder.build(MARKERS, new ChapterOptions(0, true, "Start")));
    }

    @Test
    void emptyWithoutIntroIsEmpty() {
        assertEquals("", ChapterBuilder.build(List.of(), new ChapterOptions(0, false, "")));
    }

    @Test
    void offsetTooLargeFails() {
        assertThrows(IllegalArgumentException.class,
                () -> ChapterBuilder.build(MARKERS, new ChapterOptions(5000, false, "")));
    }
}
