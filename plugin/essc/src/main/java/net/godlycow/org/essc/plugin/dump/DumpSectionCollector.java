
package net.godlycow.org.essc.plugin.dump;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.SpawnManager;
import net.godlycow.org.essc.modules.backup.BackupManager;
import net.godlycow.org.essc.modules.home.HomeManager;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import net.godlycow.org.essc.modules.warp.WarpManager;
import net.godlycow.org.essc.plugin.economy.EconomyManager;
import net.godlycow.org.essc.server.software.ServerSoftware;
import net.godlycow.org.essc.storage.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DumpSectionCollector {

    private final EssentialsC plugin;

    private final List<String> sectionNames = List.of(
            "summary", "modules", "runtime", "integrations", "config",
            "economy", "punishments", "homes", "warps", "spawn",
            "backups", "players", "logs", "health"
    );

    public DumpSectionCollector(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public List<String> getSectionNames() {
        return sectionNames;
    }

    public void collect(List<String> sections, Map<String, Object> out) {
        boolean all = sections == null || sections.isEmpty();

        out.put("generatedAt", Instant.now().toString());
        out.put("pluginVersion", plugin.getDescription().getVersion());

        if (all || sections.contains("summary")) {
            safePut(out, "summary", this::buildSummary);
        }

        if (all || sections.contains("modules")) {
            safePut(out, "modules", this::buildModules);
        }

        if (all || sections.contains("runtime")) {
            safePut(out, "runtime", this::buildRuntime);
        }

        if (all || sections.contains("integrations")) {
            safePut(out, "integrations", this::buildIntegrations);
        }

        if (all || sections.contains("config")) {
            safePut(out, "config", this::buildConfigSummary);
        }

        if (all || sections.contains("economy")) {
            safePut(out, "economy", this::buildEconomy);
        }

        if (all || sections.contains("punishments")) {
            safePut(out, "punishments", this::buildPunishments);
        }

        if (all || sections.contains("homes")) {
            safePut(out, "homes", this::buildHomes);
        }

        if (all || sections.contains("warps")) {
            safePut(out, "warps", this::buildWarps);
        }

        if (all || sections.contains("spawn")) {
            safePut(out, "spawn", this::buildSpawn);
        }

        if (all || sections.contains("backups")) {
            safePut(out, "backups", this::buildBackups);
        }

        if (all || sections.contains("players")) {
            safePut(out, "players", this::buildPlayers);
        }

        if (all || sections.contains("logs")) {
            safePut(out, "logs", this::buildLogs);
        }

        if (all || sections.contains("health")) {
            safePut(out, "health", this::buildHealth);
        }
    }

    private Map<String, Object> buildSummary() {

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("serverSoftware", ServerSoftware.get().name());
        summary.put("serverVersion", Bukkit.getVersion());
        summary.put("javaVersion", System.getProperty("java.version"));
        summary.put("onlineMode", Bukkit.getOnlineMode());
        summary.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        summary.put("maxPlayers", Bukkit.getMaxPlayers());
        summary.put("loadedWorlds", Bukkit.getWorlds().size());
        summary.put("debugEnabled", plugin.getConfigManager().isDebug());
        summary.put("uptime", formatUptime(ManagementFactory.getRuntimeMXBean().getUptime()));
        summary.put("worldNames", Bukkit.getWorlds().stream().map(World::getName).toList());

        return summary;
    }

    private Map<String, Object> buildModules() {
        Map<String, Object> modules = new LinkedHashMap<>();

        modules.put("economy", moduleStatus(plugin.getConfigManager().isEconomyEnabled(), plugin.getEconomyManager() != null));
        modules.put("homes", moduleStatus(true, plugin.getHomeManager() != null));
        modules.put("warps", moduleStatus(plugin.getConfigManager().isWarpEnabled(), plugin.getWarpManager() != null));
        modules.put("spawn", moduleStatus(true, plugin.getSpawnManager() != null));
        modules.put("back", moduleStatus(true, plugin.getBackManager() != null));
        modules.put("afk", moduleStatus(plugin.getConfigManager().isAfkEnabled(), plugin.getAfkManager() != null));
        modules.put("vanish", moduleStatus(true, plugin.getVanishManager() != null));
        modules.put("fly", moduleStatus(true, plugin.getFlyManager() != null));
        modules.put("kits", moduleStatus(true, plugin.getKitManager() != null));
        modules.put("shop", moduleStatus(plugin.getConfigManager().isShopEnabled(), plugin.getShopManager() != null));
        modules.put("auction", moduleStatus(plugin.getConfigManager().isAHEnabled(), plugin.getAuctionManager() != null));
        modules.put("sell", moduleStatus(plugin.getConfigManager().isSellEnabled(), plugin.getSellManager() != null));
        modules.put("punishments", moduleStatus(true, plugin.getPunishmentManager() != null));
        modules.put("nick", moduleStatus(plugin.getConfigManager().isNickEnabled(), plugin.getNickManager() != null));
        modules.put("scoreboard", moduleStatus(plugin.getConfigManager().isScoreboardEnabled(), plugin.getScoreboardManager() != null));
        modules.put("backup", moduleStatus(plugin.getConfigManager().isBackupEnabled(), plugin.getBackupManager() != null));
        modules.put("chat", moduleStatus(plugin.getConfigManager().isChatSystemEnabled(), plugin.getChatManager() != null));
        modules.put("rtp", moduleStatus(plugin.getConfigManager().isRTPEnabled(), plugin.getRtpManager() != null));

        return modules;
    }

    private Map<String, Object> moduleStatus(boolean enabledByConfig, boolean initialized) {
        Map<String, Object> status = new LinkedHashMap<>();

        status.put("enabledByConfig", enabledByConfig);
        status.put("initialized", initialized);
        status.put("active", enabledByConfig && initialized);


        return status;
    }

    private Map<String, Object> buildRuntime() {
        Map<String, Object> runtime = new LinkedHashMap<>();

        double[] tps = Bukkit.getServer().getTPS();
        runtime.put("tps1m", tps.length > 0 ? round(tps[0]) : null);
        runtime.put("tps5m", tps.length > 1 ? round(tps[1]) : null);
        runtime.put("tps15m", tps.length > 2 ? round(tps[2]) :null);

        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMb = rt.maxMemory() / 1024 / 1024;
        runtime.put("memoryUsedMb", usedMb);

        runtime.put("memoryMaxMb", maxMb);
        runtime.put("memoryPercent", Math.round(usedMb * 100.0 / maxMb) + "%");
        runtime.put("processors", rt.availableProcessors());
        runtime.put("folia", ServerSoftware.isFolia());
        runtime.put("paper", ServerSoftware.isPaper());
        runtime.put("os", System.getProperty("os.name"));
        runtime.put("javaVendor", System.getProperty("java.vendor"));


        return runtime;
    }

    private Map<String, Object> buildIntegrations() {
        Map<String, Object> integrations = new LinkedHashMap<>();


        integrations.put("vault", integrationStatus(plugin.isVaultHooked(),
                Bukkit.getPluginManager().getPlugin("Vault") != null));

        integrations.put("luckPerms", integrationStatus(Bukkit.getPluginManager().getPlugin("LuckPerms") != null,
                Bukkit.getPluginManager().getPlugin("LuckPerms") != null));

        integrations.put("placeholderAPI", integrationStatus(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null,
                Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null));

        integrations.put("discordSRV", integrationStatus(plugin.getDiscordSRVHook() != null,
                Bukkit.getPluginManager().getPlugin("DiscordSRV") != null));

        integrations.put("floodgate", integrationStatus(plugin.getBedrockUtil() != null,
                Bukkit.getPluginManager().getPlugin("floodgate") != null));

        integrations.put("tab", integrationStatus(Bukkit.getPluginManager().getPlugin("TAB") != null,
                Bukkit.getPluginManager().getPlugin("TAB") != null));

        integrations.put("conflictingPlugins", buildConflictingPlugins());


        return integrations;
    }

    private Map<String, Object> integrationStatus(boolean active, boolean present) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("present", present);
        status.put("active", active);


        return status;
    }

    private Map<String, Object> buildConfigSummary() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("defaultLanguage", plugin.getConfigManager().getDefaultLanguage());
        config.put("debug", plugin.getConfigManager().isDebug());
        config.put("economyEnabled", plugin.getConfigManager().isEconomyEnabled());
        config.put("homeMaxHomes", plugin.getConfigManager().getMaxHomes());
        config.put("homeCooldown", plugin.getConfigManager().getHomeCooldown());
        config.put("homeWarmup", plugin.getConfigManager().getHomeWarmup());
        config.put("warpEnabled", plugin.getConfigManager().isWarpEnabled());
        config.put("warpCooldown", plugin.getConfigManager().getWarpCooldown());
        config.put("warpWarmup", plugin.getConfigManager().getWarpWarmup());



        config.put("spawnCooldown", plugin.getConfigManager().getSpawnCooldown());
        config.put("spawnWarmup", plugin.getConfigManager().getSpawnWarmup());
        config.put("afkEnabled", plugin.getConfigManager().isAfkEnabled());
        config.put("afkTimeout", plugin.getConfigManager().getAfkTimeout());
        config.put("backupKeepLast", plugin.getConfigManager().getBackupKeepLast());
        config.put("kitMode", plugin.getConfigManager().getKitMode());
        config.put("nickEnabled", plugin.getConfigManager().isNickEnabled());
        config.put("scoreboardEnabled", plugin.getConfigManager().isScoreboardEnabled());
        return config;
    }

    private Map<String, Object> buildEconomy() {
        Map<String, Object> eco = new LinkedHashMap<>();

        if (plugin.getEconomyManager() == null) {
            eco.put("enabled", false);
            eco.put("reason", "manager not initialized");
            return eco;
        }

        EconomyManager economyManager = plugin.getEconomyManager();
        eco.put("enabled", true);
        eco.put("vaultHooked", plugin.isVaultHooked());
        eco.put("currencySingular", economyManager.currencyNameSingular());
        eco.put("currencyPlural", economyManager.currencyNamePlural());
        eco.put("startingBalance", economyManager.getStartingBalance().toPlainString());
        eco.put("minTransaction", economyManager.getMinTransaction().toPlainString());
        eco.put("maxBalance", economyManager.hasMaxBalance()
                ? economyManager.getMaxBalance().toPlainString()
                : "none");

        try (Connection conn = economyManager.getDatabase().openFreshConnection();
             Statement st = conn.createStatement()) {

            ResultSet count = st.executeQuery("SELECT COUNT(*) AS c FROM economy");
            if (count.next()) {
                eco.put("totalAccounts", count.getInt("c"));
            }

            ResultSet total = st.executeQuery("SELECT ROUND(SUM(balance), 2) AS t FROM economy");
            if (total.next()) {
                eco.put("totalMoneyInCirculation", total.getDouble("t"));
            }

            ResultSet zeroes = st.executeQuery("SELECT COUNT(*) AS c FROM economy WHERE balance = 0");
            if (zeroes.next()) {
                eco.put("accountsWithZeroBalance", zeroes.getInt("c"));
            }

            ResultSet top = st.executeQuery("SELECT username, balance FROM economy ORDER BY balance DESC LIMIT 5");
            List<String> topBalances = new ArrayList<>();
            while (top.next()) {
                topBalances.add(top.getString("username") + " — " + top.getDouble("balance"));
            }
            eco.put("top5Balances", topBalances);

        } catch (Exception e) {
            eco.put("databaseError", e.getMessage());
        }

        return eco;
    }

    private Map<String, Object> buildPunishments() {
        Map<String, Object> punishments = new LinkedHashMap<>();

        if (plugin.getPunishmentManager() == null) {
            punishments.put("enabled", false);
            punishments.put("reason", "manager not initialized");
            return punishments;
        }

        PunishmentManager manager = plugin.getPunishmentManager();
        punishments.put("enabled", true);
        punishments.put("activeBans", manager.getActiveBans().size());
        punishments.put("activeIpBans", manager.getActiveIpBans().size());
        punishments.put("activeMutes", manager.getAllMutes().size());
        punishments.put("networkHookActive", manager.getNetworkHook() != null);

        List<Map<String, Object>> banEntries = new ArrayList<>();
        for (PunishmentManager.BanEntry ban : manager.getActiveBans()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("player", ban.name());
            entry.put("uuid", ban.uuid().toString());
            entry.put("reason", ban.reason());
            entry.put("banner", ban.banner());
            entry.put("expires", ban.expires() <= 0 ? "permanent" : Instant.ofEpochMilli(ban.expires()).toString());
            banEntries.add(entry);
        }
        punishments.put("bans", banEntries);

        List<Map<String, Object>> muteEntries = new ArrayList<>();
        for (PunishmentManager.MuteEntry mute : manager.getAllMutes()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("player", mute.name());
            entry.put("uuid", mute.uuid().toString());
            entry.put("reason", mute.reason());
            entry.put("muter", mute.muter());
            entry.put("expires", mute.expires() <= 0 ? "permanent" : Instant.ofEpochMilli(mute.expires()).toString());
            muteEntries.add(entry);
        }
        punishments.put("mutes", muteEntries);

        return punishments;
    }

    private Map<String, Object> buildHomes() {
        Map<String, Object> homes = new LinkedHashMap<>();

        if (plugin.getHomeManager() == null) {
            homes.put("enabled", false);
            homes.put("reason", "manager not initialized");
            return homes;
        }

        HomeManager homeManager = plugin.getHomeManager();
        homes.put("enabled", true);

        try (Connection conn = homeManager.getDatabase().openFreshConnection();
             Statement st = conn.createStatement()) {
            ResultSet count = st.executeQuery("SELECT COUNT(*) AS c FROM homes");
            if (count.next()) {
                homes.put("totalHomes", count.getInt("c"));
            }

            ResultSet owners = st.executeQuery("SELECT COUNT(DISTINCT uuid) AS c FROM homes");
            if (owners.next()) {
                homes.put("homeOwners", owners.getInt("c"));
            }
        } catch (Exception e) {
            homes.put("databaseError", e.getMessage());
        }

        return homes;
    }

    private Map<String, Object> buildWarps() {
        Map<String, Object> warps = new LinkedHashMap<>();

        if (plugin.getWarpManager() == null) {
            warps.put("enabled", false);
            warps.put("reason", "manager not initialized");
            return warps;
        }

        WarpManager warpManager = plugin.getWarpManager();
        warps.put("enabled", true);
        warps.put("totalWarps", warpManager.getAllWarps().size());
        warps.put("visibleWarps", warpManager.getVisibleWarps().size());
        warps.put("categories", warpManager.getCategories().stream().sorted().toList());
        return warps;
    }

    private Map<String, Object> buildSpawn() {
        Map<String, Object> spawn = new LinkedHashMap<>();

        if (plugin.getSpawnManager() == null) {
            spawn.put("enabled", false);
            spawn.put("reason", "manager not initialized");

            return spawn;
        }

        SpawnManager spawnManager = plugin.getSpawnManager();
        spawn.put("enabled", true);
        spawn.put("spawnSet", spawnManager.isSpawnSet());
        spawn.put("spawnLocation", spawnManager.getSpawn());
        spawn.put("cooldown", plugin.getConfigManager().getSpawnCooldown());
        spawn.put("warmup", plugin.getConfigManager().getSpawnWarmup());

        return spawn;
    }

    private Map<String, Object> buildBackups() {
        Map<String, Object> backups = new LinkedHashMap<>();

        if (plugin.getBackupManager() == null) {
            backups.put("enabled", false);
            backups.put("reason", "manager not initialized");


            return backups;
        }

        BackupManager backupManager = plugin.getBackupManager();
        List<File> backupFiles = backupManager.listBackups();
        backups.put("enabled", true);
        backups.put("folderExists", new File(plugin.getDataFolder(), "backups").exists());
        backups.put("count", backupFiles.size());

        if (!backupFiles.isEmpty()) {
            File latest = backupFiles.get(0);
            backups.put("latestBackup", latest.getName());
            backups.put("latestBackupModified", Instant.ofEpochMilli(latest.lastModified()).toString());
            backups.put("latestBackupSizeMb", round(latest.length() / 1024.0 / 1024.0));
        }

        return backups;
    }

    private Map<String, Object> buildPlayers() {
        Map<String, Object> players = new LinkedHashMap<>();
        List<Map<String, Object>> playerEntries = new ArrayList<>();

        int afkCount = 0;
        int vanishedCount = 0;
        int flyingCount = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", player.getName());
            entry.put("uuid", player.getUniqueId().toString());
            entry.put("world", player.getWorld().getName());
            entry.put("gamemode", player.getGameMode().name());
            entry.put("health", round(player.getHealth()));
            entry.put("food", player.getFoodLevel());
            entry.put("ping", player.getPing());
            entry.put("afk", plugin.getAfkManager() != null && plugin.getAfkManager().isAFK(player));
            entry.put("vanished", plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(player));
            entry.put("flying", plugin.getFlyManager() != null && plugin.getFlyManager().isFlying(player));

            boolean pendingTeleport = false;
            if (plugin.getSpawnManager() != null && plugin.getSpawnManager().hasPendingTeleport(player)) {
                pendingTeleport = true;
            }
            if (plugin.getHomeManager() != null && plugin.getHomeManager().hasPendingTeleport(player)) {
                pendingTeleport = true;
            }
            entry.put("pendingTeleport", pendingTeleport);

            boolean muted = false;
            boolean banned = false;

            if (plugin.getPunishmentManager() != null) {
                banned = plugin.getPunishmentManager().isBanned(player.getUniqueId());
                muted = plugin.getPunishmentManager().isMuted(player.getUniqueId());
            }
            entry.put("banned", banned);
            entry.put("muted", muted);

            if (plugin.getUserManager() != null) {
                entry.put("tpaBlocked", plugin.getUserManager().isTpaBlocked(player.getUniqueId()));
            }

            playerEntries.add(entry);

            if (Boolean.TRUE.equals(entry.get("afk"))) afkCount++;
            if (Boolean.TRUE.equals(entry.get("vanished"))) vanishedCount++;
            if (Boolean.TRUE.equals(entry.get("flying"))) flyingCount++;
        }

        players.put("summary", Map.of(
                "count", playerEntries.size(),
                "afk", afkCount,
                "vanished", vanishedCount,
                "flying", flyingCount
        ));
        players.put("players", playerEntries);

        return players;
    }

    private Map<String, Object> buildLogs() {
        Map<String, Object> logs = new LinkedHashMap<>();
        File logFile = findLatestLogFile();

        if (logFile == null) {
            logs.put("available", false);


            return logs;
        }

        logs.put("available", true);
        logs.put("path", logFile.getAbsolutePath());
        logs.put("sizeKb", round(logFile.length() / 1024.0));

        try {
            List<String> lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
            int start = Math.max(0, lines.size() - 120);
            logs.put("tail", lines.subList(start, lines.size()));
        } catch (IOException e) {
            logs.put("readError", e.getMessage());
        }

        return logs;
    }

    private Map<String, Object> buildHealth() {
        List<String> warnings = new ArrayList<>();

        if (plugin.getSpawnManager() != null && !plugin.getSpawnManager().isSpawnSet()) {
            warnings.add("Spawn point is not configured");
        }

        if (plugin.getConfigManager().isEconomyEnabled() && plugin.getEconomyManager() == null) {
            warnings.add("Economy is enabled in config but the manager is not initialized");
        }

        if (plugin.getConfigManager().isWarpEnabled() && plugin.getWarpManager() == null) {
            warnings.add("Warp system is enabled in config but the manager is not initialized");
        }

        if (plugin.getConfigManager().isBackupEnabled() && plugin.getBackupManager() == null) {
            warnings.add("Backups are enabled in config but the manager is not initialized");
        }

        if (plugin.getBackupManager() != null && plugin.getBackupManager().listBackups().isEmpty()) {
            warnings.add("No backup archives were found");
        }

        if (!plugin.isVaultHooked() && plugin.getConfigManager().isEconomyEnabled()) {
            warnings.add("Economy is enabled but Vault is not hooked");
        }

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("warnings", warnings);
        return health;
    }

    private List<Map<String, Object>> buildConflictingPlugins() {
        Set<String> knownConflicts = Set.of(
                "Essentials",
                "EssentialsX",
                "CMI",
                "EssentialsXChat",
                "EssentialsXSpawn",
                "EssentialsXAntiBuild"
        );

        List<Map<String, Object>> found = new ArrayList<>();

        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", p.getName());
            entry.put("version", p.getDescription().getVersion());
            entry.put("enabled", p.isEnabled());
            if (knownConflicts.contains(p.getName())) {
                entry.put("conflict", true);
            }
            found.add(entry);
        }

        return found;
    }

    private File findLatestLogFile() {
        List<File> candidates = new ArrayList<>();

        File cwdLogs = new File("logs");
        if (cwdLogs.exists() && cwdLogs.isDirectory()) {
            candidates.addAll(listLogFiles(cwdLogs));
        }

        File dataLogs = new File(plugin.getDataFolder().getParentFile().getParentFile(), "logs");
        if (dataLogs.exists() && dataLogs.isDirectory()) {
            candidates.addAll(listLogFiles(dataLogs));
        }

        if (candidates.isEmpty()) {

            return null;
        }

        candidates.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return candidates.get(0);
    }

    private List<File> listLogFiles(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".log") || name.endsWith(".log.gz") || name.endsWith(".txt"));
        if (files == null) {
            return List.of();
        }

        List<File> result = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                result.add(file);
            }
        }
        return result;
    }

    private void safePut(Map<String, Object> map, String key, MapSupplier supplier) {
        try {
            map.put(key, supplier.get());
        } catch (Exception e) {
            map.put(key + "_error", e.getMessage());
        }
    }

    @FunctionalInterface
    private interface MapSupplier {
        Object get() throws Exception;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String formatUptime(long ms) {
        long seconds = ms / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;


        return hours + "h " + minutes + "m " + secs + "s";
    }
}
