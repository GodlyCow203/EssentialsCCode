package net.godlycow.org.essc.util;

import net.godlycow.org.essc.EssentialsC;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionCheckUtil implements Listener {

    private static final String MODRINTH_PROJECT_ID = "K7HZMVgx";
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_ID + "/version";
    private static String latestVersion = "unknown";
    private static boolean updateAvailable = false;
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern VERSION_PATTERN = Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");

    public VersionCheckUtil(EssentialsC plugin) {
        checkVersion(plugin);
    }

    private void checkVersion(EssentialsC plugin) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(MODRINTH_API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "EssentialsC/" + plugin.getDescription().getVersion());

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    throw new Exception("HTTP " + responseCode);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();
                Matcher matcher = VERSION_PATTERN.matcher(json);

                if (matcher.find()) {
                    latestVersion = matcher.group(1);
                    String currentVersion = plugin.getDescription().getVersion();
                    updateAvailable = !currentVersion.equalsIgnoreCase(latestVersion);

                    plugin.getServer().getGlobalRegionScheduler().run(plugin, task1 -> logVersionStatus(plugin, currentVersion));
                } else {
                    throw new Exception("Could not parse version from response");
                }

            } catch (Exception e) {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task1 -> {
                    plugin.getLogger().warning("================================");
                    plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
                    plugin.getLogger().warning("================================");
                });
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void logVersionStatus(EssentialsC plugin, String currentVersion) {
        plugin.getLogger().info("================================");

        if (updateAvailable) {
            plugin.getLogger().info("EssentialsC - Update Available");
            plugin.getLogger().info("--------------------------------");
            plugin.getLogger().info("Current: " + currentVersion);
            plugin.getLogger().info("Latest: " + latestVersion);
            plugin.getLogger().info("Download: https://modrinth.com/plugin/essentialsc");
        } else {
            plugin.getLogger().info("EssentialsC - Up To Date");
            plugin.getLogger().info("--------------------------------");
            plugin.getLogger().info("Running version: " + currentVersion);
        }

        plugin.getLogger().info("================================");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!updateAvailable) return;
        if (!player.hasPermission("essentialsc.version.notify") && !player.isOp()) return;

        EssentialsC plugin = JavaPlugin.getPlugin(EssentialsC.class);
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;

            player.sendMessage(MINI.deserialize("<color:#AAAAAA>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</color>"));
            player.sendMessage(MINI.deserialize("<color:#FFF200>EssentialsC Update Available</color>"));
            player.sendMessage(MINI.deserialize("<color:#AAAAAA>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</color>"));
            player.sendMessage(MINI.deserialize("<color:#AAAAAA>Current: <color:#FF0000>" + plugin.getDescription().getVersion() + "</color></color>"));
            player.sendMessage(MINI.deserialize("<color:#AAAAAA>Latest: <color:#00AA00>" + latestVersion + "</color></color>"));
            player.sendMessage(MINI.deserialize("<color:#AAAAAA>Download: <color:#66AAFF><click:open_url:'https://modrinth.com/plugin/essentialsc'><hover:show_text:'<color:#AAAAAA>Click to open Modrinth'>https://modrinth.com/plugin/essentialsc</hover></click></color></color>"));
            player.sendMessage(MINI.deserialize("<color:#AAAAAA>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</color>"));
        }, null, 20L);
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }
}