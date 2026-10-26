package com.awesomeshot5051.edl2chapters.utils.platform;

public enum OperatingSystem {
    WINDOWS,
    MAC,
    LINUX;

    public static OperatingSystem fromName(String osName) {
        String name = osName.toLowerCase();
        if (name.contains("win")) {
            return WINDOWS;
        }
        if (name.contains("mac") || name.contains("darwin")) {
            return MAC;
        }
        return LINUX;
    }

    public static OperatingSystem current() {
        return fromName(System.getProperty("os.name", "linux"));
    }
}
