package com.awesomeshot5051.edl2chapters.utils.edl;

import com.awesomeshot5051.edl2chapters.model.Marker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdlParserTest {

    private static final String BODY = """
            TITLE: T
            FCM: NON-DROP FRAME

            001  001      V     C        00:00:38:21 00:00:38:22 00:00:38:21 00:00:38:22
             |C:ResolveColorBlue |M:Café  Ünï |D:192

            002  001      V     C        01:02:03:00 01:02:03:01 01:02:03:00 01:02:03:01
             |C:ResolveColorBlue |M:No duration
            """;

    @Test
    void parsesLfAndCrlf() {
        List<Marker> expected = List.of(new Marker(38, "Café  Ünï"), new Marker(3723, "No duration"));
        assertEquals(expected, EdlParser.parse(BODY));
        assertEquals(expected, EdlParser.parse(BODY.replace("\n", "\r\n")));
    }

    @Test
    void noMarkersGivesEmptyList() {
        assertTrue(EdlParser.parse("TITLE: x\nFCM: NON-DROP FRAME\n").isEmpty());
        assertTrue(EdlParser.parse("").isEmpty());
    }
}
