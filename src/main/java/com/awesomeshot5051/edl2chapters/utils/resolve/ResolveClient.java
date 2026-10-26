package com.awesomeshot5051.edl2chapters.utils.resolve;

import com.awesomeshot5051.edl2chapters.model.Marker;
import com.awesomeshot5051.edl2chapters.model.TimelineInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Talks to the Lua bridge that Resolve runs from Workspace > Scripts (resolve/Edl2Chapters.lua).
 * Requests go out as request.lua in the mailbox folder; answers come back through Fusion.prefs.
 */
public final class ResolveClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(6);
    private static final long POLL_MILLIS = 100;
    private static final Pattern RESPONSE = Pattern.compile(
            "Edl2ChaptersBridge\\s*=\\s*\\{[^}]*?Response\\s*=\\s*\"([^\"]*)\"", Pattern.DOTALL);

    private final Path mailboxDir;
    private final Path prefsFile;
    private final Duration timeout;

    public ResolveClient() {
        this(ResolveBridgePaths.mailboxDir(), ResolveBridgePaths.fusionPrefsFile(), DEFAULT_TIMEOUT);
    }

    public ResolveClient(Path mailboxDir, Path prefsFile, Duration timeout) {
        this.mailboxDir = mailboxDir;
        this.prefsFile = prefsFile;
        this.timeout = timeout;
    }

    public List<TimelineInfo> fetchTimelines() throws IOException {
        return ResolveResponseParser.parseTimelines(ask("action = \"timelines\""));
    }

    public List<Marker> fetchMarkers(int timelineIndex) throws IOException {
        return ResolveResponseParser.parseMarkers(ask("action = \"markers\", timeline = " + timelineIndex));
    }

    private String ask(String fields) throws IOException {
        String id = Long.toString(System.currentTimeMillis() * 1000 + (System.nanoTime() % 1000));
        Files.createDirectories(mailboxDir);
        Path temp = mailboxDir.resolve("request.tmp");
        Files.writeString(temp, "return { id = \"" + id + "\", " + fields + " }\n", StandardCharsets.UTF_8);
        Files.move(temp, mailboxDir.resolve("request.lua"), StandardCopyOption.REPLACE_EXISTING);

        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            while (System.nanoTime() < deadline) {
                String body = readResponse(id);
                if (body != null) {
                    return unwrap(body);
                }
                Thread.sleep(POLL_MILLIS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
        throw new IOException("No answer from DaVinci Resolve");
    }

    private String readResponse(String id) throws IOException {
        if (!Files.isRegularFile(prefsFile)) {
            return null;
        }
        Matcher matcher = RESPONSE.matcher(Files.readString(prefsFile, StandardCharsets.ISO_8859_1));
        if (matcher.find() && matcher.group(1).startsWith(id + ":")) {
            byte[] decoded = Base64.getDecoder().decode(matcher.group(1).substring(id.length() + 1));
            return new String(decoded, StandardCharsets.UTF_8);
        }
        return null;
    }

    private static String unwrap(String body) throws IOException {
        String[] parts = body.split("\\R", 2);
        if (parts[0].equals("ERROR")) {
            throw new IOException("DaVinci Resolve reported: " + (parts.length > 1 ? parts[1] : "unknown error"));
        }
        return parts.length > 1 ? parts[1] : "";
    }
}
