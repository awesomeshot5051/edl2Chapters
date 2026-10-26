package com.awesomeshot5051.edl2chapters.utils.resolve;

import com.awesomeshot5051.edl2chapters.model.Marker;
import com.awesomeshot5051.edl2chapters.model.TimelineInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveClientTest {

    @TempDir
    Path dir;

    private Path mailbox;
    private Path prefs;
    private Thread fakeBridge;
    private volatile boolean running = true;
    private volatile boolean answering = true;

    @BeforeEach
    void startFakeBridge() {
        mailbox = dir.resolve("mailbox");
        prefs = dir.resolve("Fusion.prefs");
        fakeBridge = new Thread(this::bridgeLoop);
        fakeBridge.setDaemon(true);
        fakeBridge.start();
    }

    @AfterEach
    void stop() throws InterruptedException {
        running = false;
        fakeBridge.join();
    }

    /** Plays the Lua side: reads request.lua, answers in Resolve's Fusion.prefs format. */
    private void bridgeLoop() {
        Pattern id = Pattern.compile("id = \"(\\d+)\"");
        String lastId = "";
        while (running) {
            try {
                Path request = mailbox.resolve("request.lua");
                if (answering && Files.exists(request)) {
                    String text = Files.readString(request);
                    Matcher matcher = id.matcher(text);
                    if (matcher.find() && !matcher.group(1).equals(lastId)) {
                        lastId = matcher.group(1);
                        String body = text.contains("timelines") ? "OK\n0\tFirst\n1\tMission 16"
                                : text.contains("timeline = 2") ? "OK\n0\tSneak Peak\n38\tChannel Intro"
                                : "ERROR\nno timeline number 9";
                        String encoded = Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
                        Files.writeString(prefs, "{\n\tGlobal = {\n\t\tEdl2ChaptersBridge = {\n\t\t\tStop = \"1 t\",\n"
                                + "\t\t\tResponse = \"" + lastId + ":" + encoded + "\",\n\t\t},\n\t},\n}\n");
                    }
                }
                Thread.sleep(20);
            } catch (IOException | InterruptedException ignored) {
                // file races with the client; retry on the next pass
            }
        }
    }

    private ResolveClient client() {
        return new ResolveClient(mailbox, prefs, Duration.ofSeconds(3));
    }

    @Test
    void fetchesTimelinesWithCurrentFlag() throws IOException {
        assertEquals(List.of(new TimelineInfo(1, "First", false), new TimelineInfo(2, "Mission 16", true)),
                client().fetchTimelines());
    }

    @Test
    void fetchesMarkers() throws IOException {
        assertEquals(List.of(new Marker(0, "Sneak Peak"), new Marker(38, "Channel Intro")), client().fetchMarkers(2));
    }

    @Test
    void bridgeErrorBecomesIoException() {
        IOException error = assertThrows(IOException.class, () -> client().fetchMarkers(9));
        assertTrue(error.getMessage().contains("no timeline number 9"));
    }

    @Test
    void staleResponseIsIgnoredAndMissingBridgeTimesOut() throws IOException {
        client().fetchTimelines();
        answering = false;
        ResolveClient impatient = new ResolveClient(mailbox, prefs, Duration.ofMillis(400));
        assertThrows(IOException.class, impatient::fetchTimelines);
    }

    @Test
    void installsLuaScriptWithMailboxAndRemovesLegacyPython() throws IOException {
        Path legacy = Files.createDirectories(dir.resolve("Scripts/Edit")).resolve("Edl2Chapters.py");
        Files.writeString(legacy, "old");
        Path installed = ResolveScriptInstaller.install(dir, mailbox);
        assertEquals(dir.resolve("Scripts/Utility/Edl2Chapters.lua"), installed);
        assertFalse(Files.exists(legacy));
        String script = Files.readString(installed);
        assertTrue(script.contains("[[" + mailbox.toAbsolutePath() + "]]"));
        assertFalse(script.contains("@MAILBOX@"));
    }

    @Test
    void presenceMarkerComesAndGoes() throws IOException {
        AppPresenceMarker.create(mailbox);
        assertTrue(Files.exists(mailbox.resolve("app-running")));
        AppPresenceMarker.remove(mailbox);
        assertFalse(Files.exists(mailbox.resolve("app-running")));
    }
}
