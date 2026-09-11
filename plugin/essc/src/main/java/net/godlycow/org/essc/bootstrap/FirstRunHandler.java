package net.godlycow.org.essc.bootstrap;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;

public class FirstRunHandler implements Listener {

    private static final String MARKER_FILE = ".setup-complete";

    private final EssentialsC plugin;
    private boolean pendingNotice;

    public FirstRunHandler(EssentialsC plugin) {
        this.plugin = plugin;

        if (isFirstRun()) {
            createMarker();
            pendingNotice = true;
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            printConsoleNotice();
        }
    }

    private boolean isFirstRun() {
        return !new File(plugin.getDataFolder(), MARKER_FILE).exists();
    }

    private void createMarker() {
        File marker = new File(plugin.getDataFolder(), MARKER_FILE);
        try {
            plugin.getDataFolder().mkdirs();
            marker.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create setup marker file: " + e.getMessage());
        }
    }

    private void printConsoleNotice() {
        plugin.getLogger().info("First run detected. To get the most out of EssentialsC,");
        plugin.getLogger().info("install the following PlaceholderAPI expansions:");
        plugin.getLogger().info("  /papi ecloud download Vault");
        plugin.getLogger().info("  /papi ecloud download Player");
        plugin.getLogger().info("  /papi ecloud download Server");
        plugin.getLogger().info("  /papi ecloud download Statistic");
        plugin.getLogger().info("  /papi reload");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!pendingNotice) return;

        Player player = event.getPlayer();
        if (!player.isOp() && !player.hasPermission("essentialsc.admin")) return;

        pendingNotice = false;
        sendNotice(player, "<color:#AAAAAA>First time setup detected. Installing recommended <color:#FFFFFF>PlaceholderAPI</color> expansions...");

        String[] expansions = {"Vault", "Player", "Server", "Statistic", "LuckPerms"};

        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> {
            plugin.getLogger().info("Running PlaceholderAPI expansion installs...");
            for (String expansion : expansions) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "papi ecloud download " + expansion);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "papi reload");
            if (player.isOnline()) {
                sendNotice(player, "<color:#AAAAAA>Installed: <color:#FFFFFF>Vault, Player, Server, Statistic, LuckPerms</color>. PlaceholderAPI reloaded.");
                sendNotice(player, "<color:#AAAAAA>This message will not appear again.");
            }
        }, 200L);
    }

    private void sendNotice(Player player, String miniMessage) {
        player.sendMessage(plugin.getMiniMessage().deserialize(miniMessage));
    }
}