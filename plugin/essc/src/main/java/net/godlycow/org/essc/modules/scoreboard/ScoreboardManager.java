package net.godlycow.org.essc.modules.scoreboard;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.LegacyColorConverter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class ScoreboardManager implements Listener {
    private final EssentialsC plugin;
    private final PlaceholderProcessor processor;
    private final Map<UUID, PlayerScoreboard> boards = new HashMap<>();
    private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();

    private ScoreboardConfig config;
    private ScheduledTask updateTask;
    private final AtomicBoolean reloading = new AtomicBoolean(false);
    private final Object lock = new Object();

    private final Map<UUID, ProcessedData> placeholderCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3000L;

    private record ProcessedData(String title, String[] lines, long timestamp) {}

    public ScoreboardManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.processor = new PlaceholderProcessor(plugin);

        loadConfig();

        if (config.isEnabled()) {
            start();
            logPlaceholderStatus();
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);

        migrateOldScoreboardData();
    }

    @Deprecated(forRemoval = true, since = "4.2.6")
    // planned to be removed for 4.3.0+
    private void migrateOldScoreboardData() {
        File scoreboardsDir = new File(plugin.getDataFolder(), "scoreboards");
        if (!scoreboardsDir.exists()) return;

        Set<UUID> toDisable = new HashSet<>();

        File dataFile = new File(scoreboardsDir, "data.yml");
        if (dataFile.exists()) {
            try {
                YamlConfiguration dataConfig =
                        YamlConfiguration.loadConfiguration(dataFile);
                List<String> uuidList = dataConfig.getStringList("disabled-players");
                for (String uuidStr : uuidList) {
                    try {
                        toDisable.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to read legacy scoreboard data.yml: " + e.getMessage());
            }
        }

        File oldTxtFile = new File(scoreboardsDir, "disabled.txt");
        if (oldTxtFile.exists()) {
            try {
                Files.readAllLines(oldTxtFile.toPath()).forEach(line -> {
                    try {
                        toDisable.add(UUID.fromString(line.trim()));
                    } catch (IllegalArgumentException ignored) {}
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to read legacy scoreboard disabled.txt: " + e.getMessage());
            }
        }

        if (toDisable.isEmpty()) {
            cleanupOldScoreboardFiles(scoreboardsDir);
            return;
        }

        int migrated = 0;
        for (UUID uuid : toDisable) {
            plugin.getUserManager().findProfile(uuid).thenAccept(profile -> {
                if (profile != null) {
                    profile.setScoreboardDisabled(true);
                    profile.setUpdatedAt(System.currentTimeMillis() / 1000L);
                    plugin.getUserManager().saveAsync(profile);
                }
            });
            migrated++;
        }

        plugin.getLogger().info("Migrated " + migrated + " scoreboard disabled states to database.");

        cleanupOldScoreboardFiles(scoreboardsDir);
    }

    //new to log if papi was found or not and log ofc
    private void logPlaceholderStatus() {
        if (processor.isPapiEnabled()) {
            plugin.getLogger().info("Scoreboard: PlaceholderAPI detected - external placeholders are enabled.");
        } else {
            plugin.getLogger().info("Scoreboard: PlaceholderAPI not detected - using built-in placeholders.");
            plugin.getLogger().info("Scoreboard: For full placeholder support, install PlaceholderAPI.");
            plugin.getLogger().info("Scoreboard: See https://wiki.godlycow.org/essc/scoreboard-placeholders.html for built-in placeholders.");
        }
    }

    @Deprecated(forRemoval = true, since = "4.2.6")
    //also marked for removal as it is only used in migrateOldScoreboardData
    private void cleanupOldScoreboardFiles(File scoreboardsDir) {
        File dataFile = new File(scoreboardsDir, "data.yml");
        if (dataFile.exists()) {
            dataFile.renameTo(new File(scoreboardsDir, "data.yml.migrated"));
        }
        File oldTxtFile = new File(scoreboardsDir, "disabled.txt");
        if (oldTxtFile.exists()) {
            oldTxtFile.renameTo(new File(scoreboardsDir, "disabled.txt.migrated"));
        }
        File backupTxt = new File(scoreboardsDir, "disabled.txt.backup");
        if (backupTxt.exists()) {
            backupTxt.delete();
        }
        File[] remaining = scoreboardsDir.listFiles();
        if (remaining != null && remaining.length == 0) {
            scoreboardsDir.delete();
        }
    }

    public void loadConfig() {
        this.config = new ScoreboardConfig(plugin);
    }

    public void reload() {
        if (!reloading.compareAndSet(false, true)) {
            plugin.getLogger().warning("Scoreboard reload already in progress!");
            return;
        }

        try {
            synchronized (lock) {
                stop();

                placeholderCache.clear();
                plugin.getConfigManager().reload();
                loadConfig();

                Iterator<Map.Entry<UUID, PlayerScoreboard>> it = boards.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, PlayerScoreboard> entry = it.next();
                    try {
                        entry.getValue().destroy();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error destroying scoreboard: " + e.getMessage());
                    }
                    it.remove();
                }

                if (config.isEnabled()) {
                    Bukkit.getOnlinePlayers().forEach(this::addPlayer);
                    start();
                    plugin.debug("Scoreboard reloaded successfully");
                } else {
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        try {
                            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                        } catch (Exception ignored) {}
                    });
                    plugin.debug("Scoreboard disabled in config");
                }
            }
        } finally {
            reloading.set(false);
        }
    }

    private void start() {
        Bukkit.getOnlinePlayers().forEach(this::addPlayer);
        updateTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> updateAll(),
                10L, config.getUpdateInterval());
    }

    private void addPlayer(@NonNull Player player) {
        if (disabledPlayers.contains(player.getUniqueId())) return;
        if (!player.isOnline()) return;

        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) return;
            try {
                PlayerScoreboard existing;
                synchronized (boards) {
                    existing = boards.remove(player.getUniqueId());
                }
                if (existing != null) {
                    existing.hide(player);
                    existing.destroy();
                }

                PlayerScoreboard board = new PlayerScoreboard(player, config);
                synchronized (boards) {
                    boards.put(player.getUniqueId(), board);
                }
                board.show(player);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to add scoreboard for " + player.getName(), e);
            }
        }, null);
    }

    public void stop() {
        synchronized (lock) {
            if (updateTask != null) {
                updateTask.cancel();
                updateTask = null;
            }

            synchronized (boards) {
                Iterator<Map.Entry<UUID, PlayerScoreboard>> it = boards.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, PlayerScoreboard> entry = it.next();
                    try {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        PlayerScoreboard sb = entry.getValue();
                        if (player != null && player.isOnline()) {
                            try { sb.hide(player); } catch (Exception ignored) {}
                        }
                        sb.destroy();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error stopping scoreboard: " + e.getMessage());
                    }
                    it.remove();
                }
            }
            placeholderCache.clear();
        }
    }

    private void updateAll() {
        if (!config.isEnabled() || reloading.get()) return;

        final long now = System.currentTimeMillis();
        final List<Player> stale = new ArrayList<>();
        final List<Object[]> cached = new ArrayList<>();

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

        synchronized (boards) {
            for (Player player : onlinePlayers) {
                PlayerScoreboard board = boards.get(player.getUniqueId());
                if (board == null || !board.isActive()) continue;

                ProcessedData data = placeholderCache.get(player.getUniqueId());
                if (data != null && (now - data.timestamp()) < CACHE_TTL_MS) {
                    cached.add(new Object[]{player, board, data});
                } else {
                    stale.add(player);
                }
            }
        }

        if (!cached.isEmpty()) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                for (Object[] entry : cached) {
                    Player player = (Player) entry[0];
                    PlayerScoreboard board = (PlayerScoreboard) entry[1];
                    ProcessedData data = (ProcessedData) entry[2];
                    if (!player.isOnline() || !board.isActive()) continue;
                    board.updateProcessed(player, data.title(), Arrays.asList(data.lines()));
                }
            });
        }

        if (stale.isEmpty()) return;

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            List<ProcessedResult> results = new ArrayList<>(stale.size());

            for (Player player : stale) {
                if (!player.isOnline()) continue;
                try {
                    String title = LegacyColorConverter.toMiniMessage(
                            processor.processString(player, config.getTitleRaw()));
                    List<String> lineList = config.getLines();
                    String[] lines = new String[lineList.size()];
                    for (int i = 0; i < lineList.size(); i++) {
                        lines[i] = LegacyColorConverter.toMiniMessage(
                                processor.processString(player, lineList.get(i)));
                    }
                    results.add(new ProcessedResult(player.getUniqueId(), title, lines));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Error processing scoreboard placeholders for " + player.getName(), e);
                }
            }

            if (results.isEmpty()) return;

            final long applyTime = System.currentTimeMillis();
            for (ProcessedResult result : results) {
                placeholderCache.put(result.uuid(),
                        new ProcessedData(result.title(), result.lines(), applyTime));
            }

            plugin.getServer().getGlobalRegionScheduler().run(plugin, task1 -> {
                for (ProcessedResult result : results) {
                    Player player = Bukkit.getPlayer(result.uuid());
                    if (player == null || !player.isOnline()) continue;
                    PlayerScoreboard board;
                    synchronized (boards) {
                        board = boards.get(result.uuid());
                    }
                    if (board == null || !board.isActive()) continue;
                    board.updateProcessed(player, result.title(), Arrays.asList(result.lines()));
                }
            });
        });

        if ((now & 0x1F) == 0) {
            placeholderCache.entrySet().removeIf(e -> (now - e.getValue().timestamp()) > CACHE_TTL_MS * 2);
        }
    }

    private record ProcessedResult(UUID uuid, String title, String[] lines) {}

    public void toggle(Player player) {
        UUID uuid = player.getUniqueId();
        boolean currentlyDisabled = disabledPlayers.contains(uuid);

        if (currentlyDisabled) {
            disabledPlayers.remove(uuid);
            plugin.getUserManager().setScoreboardDisabled(uuid, false);
            addPlayer(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "scoreboard.enabled"));
        } else {
            disabledPlayers.add(uuid);
            plugin.getUserManager().setScoreboardDisabled(uuid, true);
            removePlayer(player);
            placeholderCache.remove(uuid);
            try {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            } catch (Exception e) {
                plugin.getLogger().warning("Error resetting scoreboard: " + e.getMessage());
            }
            player.sendMessage(plugin.getLanguageManager().get(player, "scoreboard.disabled"));
        }
    }

    public boolean isEnabled(Player player) {
        return !disabledPlayers.contains(player.getUniqueId());
    }

    public boolean isPlaceholderApiEnabled() {
        return processor.isPapiEnabled();
    }

    private void removePlayer(Player player) {
        PlayerScoreboard board;
        synchronized (boards) {
            board = boards.remove(player.getUniqueId());
        }
        if (board != null) {
            try {
                board.hide(player);
                board.destroy();
            } catch (Exception e) {
                plugin.getLogger().warning("Error removing scoreboard: " + e.getMessage());
            }
        }
        placeholderCache.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!config.isEnabled() || reloading.get()) return;

        Player joining = event.getPlayer();
        UUID uuid = joining.getUniqueId();

        if (plugin.getUserManager() != null) {
            boolean dbDisabled = plugin.getUserManager().isScoreboardDisabled(uuid);
            if (dbDisabled) {
                disabledPlayers.add(uuid);
            } else {
                disabledPlayers.remove(uuid);
            }
        }

        joining.getScheduler().runDelayed(plugin, task -> {
            if (joining.isOnline() && !reloading.get()) {
                addPlayer(joining);
            }
        }, null, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        removePlayer(event.getPlayer());
    }

    public void shutdown() {
        reloading.set(true);
        stop();
    }
}