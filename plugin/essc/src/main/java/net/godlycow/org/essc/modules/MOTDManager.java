package net.godlycow.org.essc.modules;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.LegacyColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class MOTDManager implements Listener {

    private final EssentialsC plugin;
    private final MiniMessage miniMessage;

    private List<String> lines;
    private boolean enabled;

    public MOTDManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        load();
        if (enabled) Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void load() {
        enabled = plugin.getConfigManager().isMotdEnabled();
        lines = plugin.getConfigManager().getMotdLines();
        plugin.debug("MOTD loaded (" + lines.size() + " lines)");
    }

    public void reload() {
        load();
        plugin.debug("MOTD reloaded");
    }

    public void shutdown() {
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled || lines == null || lines.isEmpty()) return;
        Player player = event.getPlayer();
        for (String line : lines) {
            if (line.isBlank()) {
                player.sendMessage(Component.empty());
                continue;
            }
            player.sendMessage(parseLine(line, player));
        }
    }

    private Component parseLine(String line, Player player) {
        String processed = line.replace("<player>", player.getName())
                .replace("{PLAYER}", player.getName())
                .replace("{player}", player.getName())
                .replace("{ONLINE}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        processed = LegacyColorConverter.toMiniMessage(processed);
        try {
            return miniMessage.deserialize(processed);
        } catch (Exception e) {
            plugin.debug("MiniMessage parsing failed for MOTD line, using legacy: " + e.getMessage());
            return LegacyColorConverter.fromLegacyAmpersand(processed);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean e) {
        this.enabled = e;
    }
}