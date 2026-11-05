package de.twerionalert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChatMatcherTest {
    private static final List<String> WATCHED = List.of("DerJavaBaum", "Stefanmitf");

    @ParameterizedTest
    @ValueSource(strings = {
        "<DerJavaBaum> Hallo", "DerJavaBaum: Hallo", "DerJavaBaum » Hallo",
        "[Premium] DerJavaBaum: Hallo", "Premium | DerJavaBaum » Hallo",
        "[DerJavaBaum -> mir] Hallo", "[MSG: DerJavaBaum → dir] Hallo",
        "[MSG] DerJavaBaum -> me: Hallo", "DerJavaBaum flüstert dir: Hallo",
        "DerJavaBaum whispers to you: Hello", "Von DerJavaBaum: Hallo",
        "From DerJavaBaum: Hello", "§aDerJavaBaum§r: Hallo", "derjavabaum: Hallo"
    })
    void recognizesSender(String message) {
        assertEquals("DerJavaBaum", ChatMatcher.sender(message, WATCHED).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<SomeoneElse> DerJavaBaum: Hallo", "SomeoneElse: DerJavaBaum » Hallo",
        "DerJavaBaum ist jetzt online", "DerJavaBaum123: Hallo",
        "[mir -> DerJavaBaum] Hallo", "DerJavaBaum -> SomeoneElse: Hallo",
        "Server: DerJavaBaum schreibt dir", "", "DerJavaBaum:",
        "Du hast DerJavaBaum eine Nachricht gesendet."
    })
    void ignoresMentionsOutgoingMessagesAndNotices(String message) {
        assertTrue(ChatMatcher.sender(message, WATCHED).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "MSG » DonCreeek » Dir: y",
        "§aMSG §8» §7DonCreeek §8» §aDir§8: §7y",
        "[62] [Netherite] DonCreeek: y",
        "[62] [Netherite] ♫ DonCreeek: y",
        "[62] Netherite | ♫ DonCreeek: y",
        "[62┃Netherite] ♫ DonCreeek: y",
        "[62] [Netherite] \uE001 DonCreeek: y"
    })
    void recognizesTwerionHeadersAndRankBadgeVariants(String message) {
        assertEquals("DonCreeek", ChatMatcher.sender(message, List.of("DonCreeek")).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "MSG » Du » DonCreeek: y",
        "MSG » Dir » DonCreeek: y",
        "MSG » SomeoneElse » Dir: DonCreeek",
        "[62] [Netherite] SomeoneElse: DonCreeek: y",
        "MSG » DonCreeek123 » Dir: y"
    })
    void ignoresOutgoingTwerionMessagesAndNamesInBodies(String message) {
        assertTrue(ChatMatcher.sender(message, List.of("DonCreeek")).isEmpty());
    }

    @Test void supportsEveryConfiguredPlayer() {
        for (String name : new AlertConfig().watchedPlayers)
            assertEquals(name, ChatMatcher.sender("<" + name + "> Hallo", new AlertConfig().watchedPlayers).orElseThrow());
    }

    @Test void restrictsServerBoundary() {
        assertTrue(ChatMatcher.isAllowedServer("Twerion.net:25565", "twerion.net"));
        assertTrue(ChatMatcher.isAllowedServer("play.twerion.net", "twerion.net"));
        assertFalse(ChatMatcher.isAllowedServer("eviltwerion.net", "twerion.net"));
        assertFalse(ChatMatcher.isAllowedServer("twerion.net.example.org", "twerion.net"));
        assertFalse(ChatMatcher.isAllowedServer("twerion.net", ""));
    }
}
