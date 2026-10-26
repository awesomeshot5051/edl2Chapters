package com.awesomeshot5051.edl2chapters.utils.resolve;

import java.nio.file.Path;

/** Where the app and the Resolve Lua bridge meet on disk. */
public final class ResolveBridgePaths {

    private ResolveBridgePaths() {
    }

    public static Path userHome() {
        return Path.of(System.getProperty("user.home"));
    }

    /** Folder shared with the Lua bridge: request.lua in, app-running marker. */
    public static Path mailboxDir() {
        return userHome().resolve(".local/share/edl2chapters/resolve-bridge");
    }

    public static Path resolveDataDir() {
        return userHome().resolve(".local/share/DaVinciResolve");
    }

    /** The prefs file Resolve writes (SavePrefs) and the bridge answers through. */
    public static Path fusionPrefsFile() {
        return resolveDataDir().resolve("Fusion/Profiles/Default/Fusion.prefs");
    }
}
