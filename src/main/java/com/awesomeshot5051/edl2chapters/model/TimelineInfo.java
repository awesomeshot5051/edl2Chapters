package com.awesomeshot5051.edl2chapters.model;

public record TimelineInfo(int index, String name, boolean current) {

    @Override
    public String toString() {
        return current ? name + " (current)" : name;
    }
}
