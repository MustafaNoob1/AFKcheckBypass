package de.twerionalert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

public final class AlertConfig {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean enabled = true;
    public String allowedServer = "twerion.net";
    public String applicationToken = "";
    public String userKey = "";
    public String device = "";
    public boolean includeMessage = true;
    public int cooldownSeconds = 30;
    public boolean testOnNextStart = false;
    public List<String> watchedPlayers = new ArrayList<>(List.of(
        "DerJavaBaum", "Stefanmitf", "zlxne", "viaTeamspeak", "Zuendschnur",
        "DerDieDasCrackHD", "GrafSchreiner", "silentreport", "DavoZmaj", "LeDxniel",
        "Gamingcode", "totking", "NurRidwan", "DonCreeek"
    ));

    static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("twerion-alert.json");
    }

    static AlertConfig load() throws IOException {
        Path path = path();
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            try {
                Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            } catch (UnsupportedOperationException e) {
                Files.createFile(path);
            }
            new AlertConfig().save();
        }
        AlertConfig config = JSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), AlertConfig.class);
        if (config == null || config.watchedPlayers == null || config.allowedServer == null
            || config.applicationToken == null || config.userKey == null || config.device == null
            || config.watchedPlayers.stream().anyMatch(n -> n == null || !n.matches("[A-Za-z0-9_]{3,16}"))) {
            throw new IOException("Invalid configuration");
        }
        config.cooldownSeconds = Math.max(5, Math.min(3600, config.cooldownSeconds));
        return config;
    }

    boolean hasCredentials() { return !applicationToken.isBlank() && !userKey.isBlank(); }

    void save() throws IOException {
        Files.writeString(path(), JSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8,
            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }
}
