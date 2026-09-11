package net.godlycow.org.essc.modules.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.plugin.config.EssConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager implements Listener {

    private final EssentialsC plugin;
    private final EssConfig config;
    private final ChatFormatter formatter;
    private final MentionHandler mentionHandler;

    private final Map<UUID, Long> slowModeCooldowns = new ConcurrentHashMap<>();

    public ChatManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.formatter = new ChatFormatter(plugin);
        this.mentionHandler = new MentionHandler(plugin);
        reload();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.debug("Chat Manager initialized");
    }

    public void reload() {
        if (!config.isLuckPermsChatEnabled()
                || !plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            formatter.reload(null, false);
            return;
        }

        var registration = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (registration != null) {
            formatter.reload(registration.getProvider(), true);
        } else {
            formatter.reload(null, false);
            plugin.getLogger().warning("LuckPerms service not available for chat formatting.");
        }
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!handleSlowMode(player, event))
            return;

        String raw = PlainTextComponentSerializer.plainText().serialize(event.message());
        raw = handleCapsLock(player, raw);
        Component message = formatter.formatMessage(player, raw);

        if (config.isChatMentionEnabled()) {//mentions
            message = mentionHandler.applyMentions(player, message);
        }
        event.message(message);

        if (formatter.isLuckPermsEnabled()) {
            event.renderer((source, sourceDisplayName, msg, viewer) ->
                    formatter.buildChatLine(source, msg));
        }
    }


    private boolean handleSlowMode(Player player, AsyncChatEvent event) {
        if (!config.isChatSlowModeEnabled())
            return true;
        if (player.hasPermission("essentialsc.chat.slowmode.bypass"))
            return true;

        long now = System.currentTimeMillis();
        long last = slowModeCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long delayMs = config.getChatSlowModeDelay() * 1000L;
        long remaining = (last + delayMs) - now;

        if (remaining > 0) {
            player.sendMessage(plugin.getLanguageManager().get(player, "chat.slowmode.wait",
                    Map.of("seconds", String.valueOf((remaining / 1000) + 1))));
            event.setCancelled(true);

            return false;
        }

        slowModeCooldowns.put(player.getUniqueId(), now);
        return true;
    }


    private String handleCapsLock(Player player, String message) {
        double threshold = config.getChatCapslockThreshold();

        if (threshold <= 0 || threshold > 1.0)
            return message;

        if (player.hasPermission("essentialsc.chat.caps.bypass"))
            return message;

        if (message.length() < 3)
            return message;

        int letters = 0;
        int upper = 0;
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) upper++;
            }
        }

        if (letters > 0 && (double) upper / letters >= threshold) {

            return message.toLowerCase();
        }

        return message;
    }

    // ── Lifecycle ───────────────────────────────────────────────────

    public void shutdown() {
        HandlerList.unregisterAll(this);
        slowModeCooldowns.clear();
        plugin.debug("ChatManager shut down");
    }
}
