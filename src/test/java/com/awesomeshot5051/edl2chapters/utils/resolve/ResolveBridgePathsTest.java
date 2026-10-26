package com.awesomeshot5051.edl2chapters.utils.resolve;

import com.awesomeshot5051.edl2chapters.utils.platform.OperatingSystem;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolveBridgePathsTest {

    private static final Path HOME = Path.of("home", "tester");

    @Test
    void detectsOperatingSystems() {
        assertEquals(OperatingSystem.WINDOWS, OperatingSystem.fromName("Windows 11"));
        assertEquals(OperatingSystem.MAC, OperatingSystem.fromName("Mac OS X"));
        assertEquals(OperatingSystem.LINUX, OperatingSystem.fromName("Linux"));
    }

    @Test
    void linuxPaths() {
        assertEquals(HOME.resolve(".local/share/edl2chapters/resolve-bridge"),
                ResolveBridgePaths.mailboxDir(OperatingSystem.LINUX, HOME, Map.of()));
        assertEquals(Path.of("xdg/edl2chapters/resolve-bridge"),
                ResolveBridgePaths.mailboxDir(OperatingSystem.LINUX, HOME, Map.of("XDG_DATA_HOME", "xdg")));
        assertEquals(HOME.resolve(".local/share/DaVinciResolve/Fusion"),
                ResolveBridgePaths.fusionDir(OperatingSystem.LINUX, HOME, Map.of()));
    }

    @Test
    void windowsPathsUseAppDataWithFallback() {
        Map<String, String> env = Map.of("APPDATA", "roaming", "LOCALAPPDATA", "local");
        assertEquals(Path.of("local/edl2chapters/resolve-bridge"),
                ResolveBridgePaths.mailboxDir(OperatingSystem.WINDOWS, HOME, env));
        assertEquals(Path.of("roaming/Blackmagic Design/DaVinci Resolve/Support/Fusion"),
                ResolveBridgePaths.fusionDir(OperatingSystem.WINDOWS, HOME, env));
        assertEquals(HOME.resolve("AppData/Roaming/Blackmagic Design/DaVinci Resolve/Support/Fusion"),
                ResolveBridgePaths.fusionDir(OperatingSystem.WINDOWS, HOME, Map.of()));
    }

    @Test
    void macPaths() {
        assertEquals(HOME.resolve("Library/Application Support/Blackmagic Design/DaVinci Resolve/Fusion"),
                ResolveBridgePaths.fusionDir(OperatingSystem.MAC, HOME, Map.of()));
    }
}
