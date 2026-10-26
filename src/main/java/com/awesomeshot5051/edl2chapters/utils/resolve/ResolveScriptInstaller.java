package com.awesomeshot5051.edl2chapters.utils.resolve;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes the bridge script into DaVinci Resolve's per-user Scripts menu folder (GUI only). */
public final class ResolveScriptInstaller {

    /** Utility scripts appear in Workspace > Scripts on every page (page folders like Edit only show on their page). */
    private static final String SCRIPT_FOLDER = "Scripts/Utility";
    /** Earlier versions installed a Python script into these places; remove it. */
    private static final List<String> LEGACY_SCRIPTS = List.of(
            "Scripts/Edit/Edl2Chapters.py", "Scripts/Utility/Edl2Chapters.py");
    public static final String SCRIPT_NAME = "Edl2Chapters.lua";
    private static final String RESOURCE = "/resolve/" + SCRIPT_NAME;
    private static final String MAILBOX_PLACEHOLDER = "@MAILBOX@";

    private ResolveScriptInstaller() {
    }

    /** Installs or refreshes the script with the mailbox folder filled in (forward slashes, which Windows accepts too); returns its path. */
    public static Path install(Path fusionDir, Path mailboxDir) throws IOException {
        Path target = fusionDir.resolve(SCRIPT_FOLDER).resolve(SCRIPT_NAME);
        for (String legacy : LEGACY_SCRIPTS) {
            Files.deleteIfExists(fusionDir.resolve(legacy));
        }
        String script;
        try (InputStream in = ResolveScriptInstaller.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IOException("Bundled script missing: " + RESOURCE);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace(MAILBOX_PLACEHOLDER, mailboxDir.toAbsolutePath().toString().replace('\\', '/'));
        }
        if (!Files.exists(target) || !Files.readString(target, StandardCharsets.UTF_8).equals(script)) {
            Files.createDirectories(target.getParent());
            Files.writeString(target, script, StandardCharsets.UTF_8);
        }
        return target;
    }

    /** Best effort at GUI start-up: only if Resolve has been run for this user. */
    public static void installIfResolvePresent() {
        Path fusionDir = ResolveBridgePaths.fusionDir();
        if (Files.isDirectory(fusionDir)) {
            try {
                install(fusionDir, ResolveBridgePaths.mailboxDir());
            } catch (IOException ignored) {
                // optional convenience
            }
        }
    }
}
