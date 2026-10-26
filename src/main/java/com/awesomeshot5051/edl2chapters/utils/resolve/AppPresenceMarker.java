package com.awesomeshot5051.edl2chapters.utils.resolve;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Keeps an "app-running" file in the mailbox while the GUI is open; the Resolve bridge stops when it vanishes. */
public final class AppPresenceMarker {

    public static final String FILE_NAME = "app-running";

    private AppPresenceMarker() {
    }

    public static void create(Path mailboxDir) throws IOException {
        Files.createDirectories(mailboxDir);
        Files.writeString(mailboxDir.resolve(FILE_NAME), Long.toString(ProcessHandle.current().pid()));
    }

    public static void remove(Path mailboxDir) {
        try {
            Files.deleteIfExists(mailboxDir.resolve(FILE_NAME));
        } catch (IOException ignored) {
            // nothing useful to do while shutting down
        }
    }

    /** Creates the marker and removes it when the JVM exits. */
    public static void createUntilExit(Path mailboxDir) {
        try {
            create(mailboxDir);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> remove(mailboxDir)));
        } catch (IOException ignored) {
            // import from DaVinci just won't connect; the rest of the app is unaffected
        }
    }
}
