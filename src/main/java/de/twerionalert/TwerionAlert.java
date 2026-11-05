package de.twerionalert;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class TwerionAlert implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("twerion-alert");
    private static final Map<String, Long> LAST_SENT = new HashMap<>();
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(16), task -> {
            Thread thread = new Thread(task, "twerion-alert-pushover");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    private static final PushoverClient PUSH = new PushoverClient();
    private static AlertConfig config;
    private static long globalLastSent;

    @Override
    public void onInitializeClient() {
        try {
            config = AlertConfig.load();
            if (config.testOnNextStart) {
                // Consume the switch first so a restart never repeats the test accidentally.
                config.testOnNextStart = false;
                config.save();
                if (config.enabled && config.hasCredentials()) {
                    enqueue("Twerion Alert – Test", "Die Verbindung zu Pushover funktioniert.");
                } else LOG.warn("Pushover test skipped: enable notifications and configure credentials first.");
            }
            LOG.info("Twerion Alert loaded. Configuration: config/twerion-alert.json");
            if (!config.hasCredentials()) LOG.warn("Pushover credentials are missing; notifications are inactive.");
        } catch (Exception e) {
            config = null;
            LOG.error("Cannot load Twerion Alert configuration. Check JSON and file permissions; notifications are inactive.");
        }
    }

    static void onChat(String message) {
        try {
            if (config == null || !config.enabled || !config.hasCredentials()) return;
            var minecraft = Minecraft.getInstance();
            var server = minecraft.getCurrentServer();
            if (server == null || !ChatMatcher.isAllowedServer(server.ip, config.allowedServer)) return;
            var sender = ChatMatcher.sender(message, config.watchedPlayers);
            if (sender.isEmpty()) return;
            String name = sender.get();
            if (minecraft.player != null && name.equalsIgnoreCase(minecraft.player.getGameProfile().name())) return;
            long now = System.nanoTime();
            long cooldown = TimeUnit.SECONDS.toNanos(config.cooldownSeconds);
            Long previous = LAST_SENT.get(name);
            if ((previous != null && now - previous < cooldown)
                || (globalLastSent != 0 && now - globalLastSent < TimeUnit.SECONDS.toNanos(5))) return;
            if (enqueue("Minecraft: " + name, config.includeMessage ? message : name + " hat auf Twerion geschrieben.")) {
                LAST_SENT.put(name, now);
                globalLastSent = now;
            }
        } catch (RuntimeException e) {
            LOG.warn("Chat notification could not be processed.");
        }
    }

    private static boolean enqueue(String title, String message) {
        try {
            WORKER.execute(() -> {
                try {
                    PUSH.send(config, title, message);
                    LOG.info("Pushover notification sent.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    LOG.warn("Pushover delivery failed. Check connectivity, credentials and Pushover quota. No automatic retry.");
                }
            });
            return true;
        } catch (java.util.concurrent.RejectedExecutionException e) {
            LOG.warn("Pushover queue full; notification dropped.");
            return false;
        }
    }
}
