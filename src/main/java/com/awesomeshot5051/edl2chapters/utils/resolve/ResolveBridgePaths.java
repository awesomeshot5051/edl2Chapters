package com.awesomeshot5051.edl2chapters.utils.resolve;

import com.awesomeshot5051.edl2chapters.utils.platform.OperatingSystem;

import java.nio.file.Path;
import java.util.Map;

/** Where the app and the Resolve Lua bridge meet on disk, per operating system. */
public final class ResolveBridgePaths {

    private static final String APP_FOLDER = "edl2chapters";
    private static final String MAILBOX_FOLDER = "resolve-bridge";
    private static final String BLACKMAGIC = "Blackmagic Design/DaVinci Resolve";

    private ResolveBridgePaths() {
    }

    /** Folder shared with the Lua bridge: request.lua in, app-running marker. */
    public static Path mailboxDir() {
        return mailboxDir(OperatingSystem.current(), userHome(), System.getenv());
    }

    /** Resolve's per-user Fusion folder: holds Scripts/ and Profiles/Default/Fusion.prefs. */
    public static Path fusionDir() {
        return fusionDir(OperatingSystem.current(), userHome(), System.getenv());
    }

    /** The prefs file Resolve writes (SavePrefs) and the bridge answers through. */
    public static Path fusionPrefsFile() {
        return fusionDir().resolve("Profiles/Default/Fusion.prefs");
    }

    public static Path mailboxDir(OperatingSystem os, Path home, Map<String, String> env) {
        return switch (os) {
            case WINDOWS -> base(env.get("LOCALAPPDATA"), home.resolve("AppData/Local")).resolve(APP_FOLDER).resolve(MAILBOX_FOLDER);
            case MAC -> home.resolve("Library/Application Support").resolve(APP_FOLDER).resolve(MAILBOX_FOLDER);
            case LINUX -> base(env.get("XDG_DATA_HOME"), home.resolve(".local/share")).resolve(APP_FOLDER).resolve(MAILBOX_FOLDER);
        };
    }

    public static Path fusionDir(OperatingSystem os, Path home, Map<String, String> env) {
        return switch (os) {
            case WINDOWS -> base(env.get("APPDATA"), home.resolve("AppData/Roaming")).resolve(BLACKMAGIC).resolve("Support/Fusion");
            case MAC -> home.resolve("Library/Application Support").resolve(BLACKMAGIC).resolve("Fusion");
            case LINUX -> home.resolve(".local/share/DaVinciResolve/Fusion");
        };
    }

    private static Path base(String fromEnv, Path fallback) {
        return fromEnv == null || fromEnv.isBlank() ? fallback : Path.of(fromEnv);
    }

    private static Path userHome() {
        return Path.of(System.getProperty("user.home"));
    }
}
