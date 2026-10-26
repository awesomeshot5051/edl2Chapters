package com.awesomeshot5051.edl2chapters.model;

public record ChapterOptions(long offsetSeconds, boolean includeIntro, String introTitle) {

    public static final String DEFAULT_INTRO_TITLE = "Intro";

    public static ChapterOptions defaults() {
        return new ChapterOptions(0, true, DEFAULT_INTRO_TITLE);
    }
}
