# Twerion Alert

Client-Mod für **Minecraft 1.21.11 / Fabric**, die Chatnachrichten ausgewählter Spieler
an die **Pushover-App** schickt. Auf dem Server muss nichts installiert werden.
Fabric API ist nicht erforderlich. Die Mod benötigt einen laufenden, verbundenen
Minecraft-Client; wenn Minecraft geschlossen ist, empfängt sie keine Nachrichten.

## Stand

Der Quellcode und automatisierte Tests sind angelegt. **Der vollständige Mod-Build,
die JUnit-Ausführung und der Spielstart sind noch nicht verifiziert**: In der Cloud
fehlt Gradle und der konfigurierte Netzwerk-Proxy war zuletzt nicht erreichbar.
Der Befehl `javac` fehlt ebenfalls, das Compiler-Modul ist jedoch verfügbar:
Mit `java -m jdk.compiler/com.sun.tools.javac.Main` wurde die tatsächliche
`ChatMatcher`-Klasse kompiliert und mit einem separaten Java-Prüfprogramm geprüft.
Alle 50 Nachrichtenfälle (einschließlich aller 14 Spielernamen) und fünf
Servergrenzen-Prüfungen bestanden. Dies ersetzt keinen vollständigen Fabric-Build.
Auch die ausgewählten
Loom-/Loader-Versionen müssen beim ersten erfolgreichen Build aufgelöst werden.
Es gibt noch keine geprüfte fertige JAR. Die Erkennung wurde anhand der
bereitgestellten Twerion-Screenshots ergänzt; ein Live-Test steht noch aus.

## Bauen

Benötigt: **JDK 21** (einschließlich `javac`), **Gradle 9.2.1** und Internetzugang.
Im Repository ausführen:

```sh
gradle --no-daemon --max-workers=2 build
```

Der Build führt die JUnit-Tests aus und erzeugt `build/libs/twerion-alert-1.0.0.jar`.
Die Datei mit `-sources` ist Quellcode, keine installierbare Mod.
Ein Gradle Wrapper ist noch nicht enthalten. Der GitHub-Workflow baut nach Push
oder manueller Ausführung und stellt bei Erfolg JARs unter **Actions → Build mod →
Artifacts → twerion-alert** bereit. Er wurde in dieser Sitzung nicht ausgeführt.

## Einrichten

1. Fabric für Minecraft **1.21.11** mit Loader **0.18.4 oder neuer** installieren.
2. Die gebaute Mod-JAR in den `mods`-Ordner dieser Minecraft-Installation legen.
3. Minecraft einmal starten und wieder schließen. Die Mod erstellt
   `config/twerion-alert.json` im Spielverzeichnis.
4. Pushover auf dem Handy installieren, Konto einrichten und auf pushover.net
   einen Application/API Token anlegen. Pushover kann nach der Testphase kostenpflichtig sein.
5. In der lokalen JSON-Datei `applicationToken` und `userKey` eintragen.
   Diese Schlüssel nicht in Chat, Screenshots oder Git veröffentlichen.
6. Optional `device` auf den Pushover-Gerätenamen setzen; leer bedeutet alle
   Geräte dieses Kontos. Für einen Verbindungstest `testOnNextStart` auf `true`
   setzen und Minecraft neu starten. Der Schalter wird vor dem Test zurückgesetzt.
   Diese Testnachricht benötigt keine Serververbindung. Erfolg/Fehler stehen in
   `logs/latest.log`; Schlüssel und Nachrichteninhalte werden dort von der Mod
   nicht ausgegeben.
7. Twerion betreten und einen der überwachten Spieler eine Nachricht schicken lassen.

Änderungen an der Konfiguration benötigen einen **Neustart von Minecraft**.
Fehlerhafte JSON-Dateien werden nicht überschrieben; Benachrichtigungen bleiben
bis zur Korrektur deaktiviert.

## Konfiguration

| Feld | Bedeutung |
| --- | --- |
| `enabled` | Benachrichtigungen an/aus, standardmäßig `true` |
| `allowedServer` | Nur dieser Server und seine Subdomains; Standard `twerion.net` |
| `applicationToken`, `userKey` | Eigene Pushover-Zugangsdaten, anfangs leer |
| `device` | Optionales Pushover-Zielgerät |
| `includeMessage` | `true`: Chattext mitsenden; `false`: nur den Spielernamen melden |
| `cooldownSeconds` | Pause pro Spieler, Standard 30 Sekunden; mindestens 5, höchstens 3600 |
| `testOnNextStart` | Eine einmalige Testnachricht beim nächsten Start auslösen |
| `watchedPlayers` | Liste der überwachten Minecraft-Namen, Groß-/Kleinschreibung egal |

Voreingestellt sind DerJavaBaum, Stefanmitf, zlxne, viaTeamspeak, Zuendschnur,
DerDieDasCrackHD, GrafSchreiner, silentreport, DavoZmaj, LeDxniel, Gamingcode,
totking, NurRidwan und DonCreeek.

## Erkennung und Grenzen

Die Mod meldet **jede erkannte öffentliche Nachricht** eines überwachten Spielers,
auch ohne Erwähnung deines Namens, und unterstützte eingehende Privatnachrichten.
Beispiele: `<DerJavaBaum> Hallo`, `[Premium] DerJavaBaum: Hallo`,
`Premium | DerJavaBaum » Hallo`, `[DerJavaBaum -> mir] Hallo`,
`[MSG] DerJavaBaum -> dir: Hallo` und `DerJavaBaum flüstert dir: Hallo`.
Bloße Erwähnungen eines Namens, Online-Meldungen und ausgehende MSG-Formate
werden nicht als Absender gewertet. Eigene Nachrichten werden ignoriert.

Für Twerion ist außerdem `MSG » DonCreeek » Dir: y` als eingehende Privatnachricht
unterstützt. Öffentliche Nachrichten dürfen Level-/Rangklammern sowie ein
Symbol vor dem Namen enthalten, beispielsweise `[62] [Netherite] ♫ DonCreeek: y`.
Die Screenshots zeigen diese Headerstruktur; die exakten kopierten Chattexte und
ein Test im Spiel müssen die Erkennung noch bestätigen. Die Tests decken auch
Varianten des Rangtrenners sowie ausgehende Nachrichten und bloße Erwähnungen ab.
Private Formate mit deinem konkreten Namen als Empfänger sind
noch nicht unterstützt. Die Mod wertet den sichtbaren Text aus; das ist keine
kryptografische Bestätigung des Absenders.

Die Mod sendet asynchron an `https://api.pushover.net/1/messages.json` und blockiert
den Spielthread nicht mit Netzwerkanfragen. Höchstens eine Nachricht alle fünf
Sekunden insgesamt, dazu die Pause pro Spieler. Während dieser Pausen werden
weitere Nachrichten verworfen. Die Warteschlange ist auf 16 Einträge begrenzt.
Bei Netzwerkfehlern oder erschöpftem Pushover-Kontingent erfolgen keine automatischen
Wiederholungen. Mit `includeMessage: true` gelangen die erkannten Chattexte an
Pushover und können auf dem Handy-Sperrbildschirm erscheinen.

## Entwicklung

`gradle test` prüft Absendererkennung, ignorierte Nachrichten, alle 14 Spielernamen
und Servergrenzen. `gradle runClient` startet den Fabric-Entwicklungsclient und
benötigt eine grafische Sitzung; das ersetzt keinen Test auf Twerion mit einem
berechtigten Minecraft-Konto und eigenen Pushover-Schlüsseln.
