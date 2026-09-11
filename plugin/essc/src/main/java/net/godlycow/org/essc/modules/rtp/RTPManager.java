package net.godlycow.org.essc.modules.rtp;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.rtp.event.RtpCooldownExpireEvent;
import net.godlycow.org.essc.api.rtp.event.RtpFailEvent;
import net.godlycow.org.essc.api.rtp.event.RtpPostTeleportEvent;
import net.godlycow.org.essc.api.rtp.event.RtpRequestEvent;
import net.godlycow.org.essc.api.rtp.event.RtpSearchCompleteEvent;
import net.godlycow.org.essc.api.rtp.event.RtpSearchStartEvent;
import net.godlycow.org.essc.api.rtp.event.RtpTeleportEvent;
import net.godlycow.org.essc.api.rtp.event.RtpWarmupCancelEvent;
import net.godlycow.org.essc.api.rtp.event.RtpWarmupStartEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class RTPManager {

    private final EssentialsC plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> pendingTeleports = new ConcurrentHashMap<>();
    private final Set<UUID> rtpInProgress = ConcurrentHashMap.newKeySet();
    private final Map<String, WorldRTPSettings> worldSettings = new ConcurrentHashMap<>();

    private boolean enabled;
    private long cooldown;
    private long warmup;
    private boolean cancelOnMovement;
    private boolean particles;
    private int maxAttempts;
    private int minY;
    private int maxY;
    private boolean useBorder;

    // nether shit
    private final int[][] netherSearchRanges = {
            {100, 122},
            {70, 95},
            {35, 60},
    };

    private final Set<Material> unsafeGround = EnumSet.of(
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE,
            Material.BEDROCK,
            Material.BARRIER
    );

    private final Set<Material> unsafeNearby = EnumSet.of(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.MAGMA_BLOCK
    );

    private final Set<Material> passableBlocks = EnumSet.of(
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.NETHER_SPROUTS,
            Material.CRIMSON_ROOTS,
            Material.WARPED_ROOTS,
            Material.TWISTING_VINES,
            Material.TWISTING_VINES_PLANT,
            Material.WEEPING_VINES,
            Material.WEEPING_VINES_PLANT,
            Material.VINE,
            Material.TALL_GRASS,
            Material.GRASS_BLOCK,
            Material.FERN,
            Material.LARGE_FERN,
            Material.DEAD_BUSH,
            Material.SNOW,
            Material.NETHER_PORTAL
    );

    public RTPManager(EssentialsC plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = plugin.getConfigManager().isRTPEnabled();
        this.cooldown = plugin.getConfigManager().getRTPCooldown();
        this.warmup = plugin.getConfigManager().getRTPWarmup();
        this.cancelOnMovement = plugin.getConfigManager().isRTPCancelOnMovement();
        this.particles = plugin.getConfigManager().isRTPParticles();
        this.maxAttempts = plugin.getConfigManager().getRTPMaxAttempts();
        this.minY = plugin.getConfigManager().getRTPMinY();
        this.maxY = plugin.getConfigManager().getRTPMaxY();
        this.useBorder = plugin.getConfigManager().isRTPUseWorldBorder();

        int globalMinRadius = plugin.getConfigManager().getRTPGMinRadius();
        int globalMaxRadius = plugin.getConfigManager().getRTPGMaxRadius();
        List<String> globalBlockedBiomes = plugin.getConfigManager().getGlobalBlockedBiomes();

        worldSettings.clear();
        var worldsSection = config.getConfigurationSection("rtp.worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                String path = "rtp.worlds." + worldName;
                int minRadius = config.getInt(path + ".min-radius", globalMinRadius);
                int maxRadius = config.getInt(path + ".max-radius", globalMaxRadius);
                List<String> blockedBiomes = config.getStringList(path + ".blocked-biomes");
                if (blockedBiomes.isEmpty()) blockedBiomes = globalBlockedBiomes;

                boolean worldEnabled = config.getBoolean(path + ".enabled", true);

                String displayName = config.getString(path + ".display-name", defaultDisplayName(worldName));
                worldSettings.put(worldName, new WorldRTPSettings(minRadius, maxRadius, blockedBiomes, worldEnabled, displayName));
            }
        }
    }

    private String defaultDisplayName(String worldName) {
        return Arrays.stream(worldName.replace("_", " ").split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public List<String> getConfiguredWorldNames() {
        return worldSettings.keySet().stream()
                .filter(name -> resolveWorld(name) != null).sorted().collect(Collectors.toList());
    }

    public World resolveWorld(String configuredName) {
        World exact = Bukkit.getWorld(configuredName);
        if (exact != null)
            return exact;
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().endsWith("-" + configuredName))

                return world;
        }

        return null;
    }

    public void reload() {
        loadConfig();
        plugin.debug("RTP configuration reloaded");
    }

    public void shutdown() {
        pendingTeleports.values().forEach(ScheduledTask::cancel);
        pendingTeleports.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isWorldEnabled(String worldName) {
        WorldRTPSettings settings = worldSettings.get(worldName);
        return settings != null && settings.enabled();
    }

    public WorldRTPSettings getWorldSettings(String worldName) {
        WorldRTPSettings settings = worldSettings.get(worldName);
        if (settings != null)
            return settings;
        if (!worldSettings.isEmpty()) return worldSettings.values().iterator().next();

        return new WorldRTPSettings(100, 10000, Collections.emptyList(), true, defaultDisplayName(worldName));
    }

    public boolean isOnCooldown(Player player) {
        if (hasBypassPermission(player, "cooldown"))
            return false;
        Long cooldownStart = cooldowns.get(player.getUniqueId());
        if (cooldownStart == null)

            return false;
        boolean expired = System.currentTimeMillis() - cooldownStart >= cooldown * 1000L;
        if (expired) {
            cooldowns.remove(player.getUniqueId());
            Bukkit.getPluginManager().callEvent(new RtpCooldownExpireEvent(player, cooldownStart));

            return false;
        }
        return true;
    }

    public long getRemainingCooldown(Player player) {
        if (hasBypassPermission(player, "cooldown")) return 0;
        Long cooldownStart = cooldowns.get(player.getUniqueId());
        if (cooldownStart == null)
            return 0;

        long elapsed = System.currentTimeMillis() - cooldownStart;
        if (elapsed >= cooldown * 1000L) {
            cooldowns.remove(player.getUniqueId());
            Bukkit.getPluginManager().callEvent(new RtpCooldownExpireEvent(player, cooldownStart));

            return 0;
        }
        return cooldown - (elapsed / 1000L);
    }

    public boolean isRtpInProgress(Player player) {

        return rtpInProgress.contains(player.getUniqueId());
    }

    public boolean hasBypassPermission(Player player, String type) {

        return player.hasPermission("essentialsc.rtp.bypass." + type);
    }

    public boolean hasWorldPermission(Player player, String worldName) {
        String worldKey = worldName.toLowerCase().replace(" ", "_").replace("world_", "");

        return player.hasPermission("essentialsc.rtp.world." + worldKey) || player.hasPermission("essentialsc.rtp.world.*");
    }

    public void startRTP(Player player, World world) {
        if (!player.hasPermission("essentialsc.rtp")) {
            player.sendMessage(plugin.getLanguageManager().get(player, "rtp.error.no_permission"));
            Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.NO_PERMISSION));


            return;
        }

        if (!hasWorldPermission(player, world.getName())) {
            player.sendMessage(plugin.getLanguageManager().get(player, "rtp.error.no_world_permission"));
            Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.NO_WORLD_PERMISSION));

            return;
        }

        if (rtpInProgress.contains(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().get(player, "rtp.error.in_progress"));
            Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.ALREADY_IN_PROGRESS));

            return;
        }

        if (isOnCooldown(player)) {
            long remaining = getRemainingCooldown(player);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(remaining));
            player.sendMessage(plugin.getLanguageManager().get(player, "rtp.error.cooldown", placeholders));
            Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.COOLDOWN_ACTIVE));
            return;


        }

        if (!isWorldEnabled(world.getName())) {
            player.sendMessage(plugin.getLanguageManager().get(player, "rtp.error.world_disabled"));
            Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.WORLD_DISABLED));
            return;

        }

        RtpRequestEvent requestEvent = new RtpRequestEvent(player, world);
        Bukkit.getPluginManager().callEvent(requestEvent);
        if (requestEvent.isCancelled()) {
            Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.EVENT_CANCELLED, requestEvent.getCancelReason()));

            return;
        }

        rtpInProgress.add(player.getUniqueId());
        long actualWarmup = hasBypassPermission(player, "warmup") ? 0L : warmup;

        if (actualWarmup > 0) {
            startWarmup(player, world, actualWarmup);
        } else {
            executeRTP(player, world);
        }
    }

    private void startWarmup(Player player, World world, long actualWarmup) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", String.valueOf(actualWarmup));
        player.sendMessage(plugin.getLanguageManager().get(player, "rtp.warmup.start", placeholders));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

        Location initialLocation = player.getLocation().clone();

        RtpWarmupStartEvent warmupEvent = new RtpWarmupStartEvent(player, world, actualWarmup);
        Bukkit.getPluginManager().callEvent(warmupEvent);
        if (warmupEvent.isCancelled()) {
            rtpInProgress.remove(player.getUniqueId());
            Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.EVENT_CANCELLED, warmupEvent.getCancelReason()));

            return;
        }

        long resolvedWarmup = warmupEvent.getWarmupSeconds();

        AtomicLong seconds = new AtomicLong(resolvedWarmup);
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, task1 -> {
            if (!player.isOnline()) {
                cleanupWarmup(player);
                Bukkit.getPluginManager().callEvent(new RtpWarmupCancelEvent(player, world, RtpWarmupCancelEvent.CancelReason.PLAYER_OFFLINE));
                Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.WARMUP_CANCELLED));

                return;
            }

            if (cancelOnMovement && !hasBypassPermission(player, "movement") && hasMoved(initialLocation, player.getLocation())) {
                cleanupWarmup(player);
                player.sendMessage(plugin.getLanguageManager().get(player, "rtp.warmup.cancelled"));
                Bukkit.getPluginManager().callEvent(new RtpWarmupCancelEvent(player, world, RtpWarmupCancelEvent.CancelReason.PLAYER_MOVED));
                Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.WARMUP_CANCELLED));


                return;
            }

            if (seconds.decrementAndGet() <= 0) {
                ScheduledTask t = pendingTeleports.remove(player.getUniqueId());
                if (t != null) t.cancel();
                executeRTP(player, world);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);
                Map<String, String> ph = new HashMap<>();
                ph.put("time", String.valueOf(seconds));
                player.sendActionBar(plugin.getLanguageManager().get(player, "rtp.warmup.actionbar", ph));
            }
        }, null, 20L, 20L);

        pendingTeleports.put(player.getUniqueId(), task);
    }

    private void cleanupWarmup(Player player) {
        rtpInProgress.remove(player.getUniqueId());
        ScheduledTask t = pendingTeleports.remove(player.getUniqueId());

        if (t != null) t.cancel();
    }

    private boolean hasMoved(Location from, Location to) {

        return from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ();
    }

    private void executeRTP(Player player, World world) {
        player.sendMessage(plugin.getLanguageManager().get(player, "rtp.searching"));

        RtpSearchStartEvent searchStartEvent = new RtpSearchStartEvent(player, world);
        Bukkit.getPluginManager().callEvent(searchStartEvent);
        if (searchStartEvent.isCancelled()) {
            rtpInProgress.remove(player.getUniqueId());
            Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.EVENT_CANCELLED, searchStartEvent.getCancelReason()));

            return;
        }

        findSafeLocation(world).thenAccept(searchResult ->
                player.getScheduler().run(plugin, task -> {
                    if (!player.isOnline()) {
                        rtpInProgress.remove(player.getUniqueId());

                        return;
                    }

                    Bukkit.getPluginManager().callEvent(
                            new RtpSearchCompleteEvent(player, world, searchResult.location(), searchResult.attempts())
                    );

                    if (searchResult.location() == null) {
                        player.sendMessage(plugin.getLanguageManager().get(player, "rtp.error.no_safe_location"));
                        rtpInProgress.remove(player.getUniqueId());

                        Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.NO_SAFE_LOCATION));
                        return;
                    }

                    Location finalLoc = searchResult.location().clone();
                    finalLoc.setYaw(random.nextFloat() * 360f);
                    finalLoc.setPitch(0f);

                    RtpTeleportEvent teleportEvent = new RtpTeleportEvent(player, world, finalLoc);
                    Bukkit.getPluginManager().callEvent(teleportEvent);
                    if (teleportEvent.isCancelled()) {
                        rtpInProgress.remove(player.getUniqueId());
                        Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.EVENT_CANCELLED, teleportEvent.getCancelReason()));


                        return;
                    }

                    Location destination = teleportEvent.getDestination();

                    plugin.teleportHelper().teleportAsync(player, destination).thenAccept(success -> {
                        if (!success) {
                            rtpInProgress.remove(player.getUniqueId());
                            Bukkit.getPluginManager().callEvent(new RtpFailEvent(player, world, RtpFailEvent.FailureReason.TELEPORT_FAILED));

                            return;
                        }

                        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                        rtpInProgress.remove(player.getUniqueId());

                        if (plugin.getUserManager() != null) {
                            plugin.getUserManager()
                                    .setRtpLastUsed(player.getUniqueId(), System.currentTimeMillis() / 1000L);
                        }

                        Bukkit.getPluginManager().callEvent(new RtpPostTeleportEvent(player, world, destination));

                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("x", String.valueOf(destination.getBlockX()));
                        placeholders.put("y", String.valueOf(destination.getBlockY()));
                        placeholders.put("z", String.valueOf(destination.getBlockZ()));

                        placeholders.put("world", world.getName());
                        player.sendMessage(plugin.getLanguageManager().get(player, "rtp.success", placeholders));

                        if (particles) spawnTeleportParticles(destination);

                        world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    });
                }, null)
        );
    }

    //find spot
    private CompletableFuture<SearchResult> findSafeLocation(World world) {
        WorldRTPSettings settings = getWorldSettings(world.getName());
        Location spawn = world.getSpawnLocation();

        int attempts = maxAttempts;

        if (world.getEnvironment() == World.Environment.NETHER) {
            attempts = maxAttempts * 3;
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            attempts = maxAttempts * 3;
        }

        return findSafeIterative(world, settings, spawn, attempts);
    }

    private CompletableFuture<SearchResult> findSafeIterative(World world, WorldRTPSettings settings, Location spawn, int totalAttempts) {

        CompletableFuture<SearchResult> result = new CompletableFuture<>();
        attemptNext(world, settings, spawn, totalAttempts, 0, result);

        return result;
    }

    private void attemptNext(World world, WorldRTPSettings settings, Location spawn, int totalAttempts, int attempt, CompletableFuture<SearchResult> result) {
        if (attempt >= totalAttempts) {
            result.complete(new SearchResult(null, attempt));

            return;
        }

        double angle = random.nextDouble() * Math.PI * 2;
        int spread = settings.maxRadius() > settings.minRadius() ? settings.minRadius() + random.nextInt(settings.maxRadius() - settings.minRadius() + 1) : settings.minRadius();

        int x = spawn.getBlockX() + (int) (Math.cos(angle) * spread);

        int z = spawn.getBlockZ() + (int) (Math.sin(angle) * spread);

        if (useBorder && isOutsideBorder(world, x, z)) {
            attemptNext(world, settings, spawn, totalAttempts, attempt + 1, result);

            return;
        }

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Location loc = resolveCandidate(world, settings, x, z);

            if (loc == null) {
                attemptNext(world, settings, spawn, totalAttempts, attempt + 1, result);

                return;
            }

            result.complete(new SearchResult(loc, attempt + 1));
        }).exceptionally(ex -> {
            attemptNext(world, settings, spawn, totalAttempts, attempt + 1, result);

            return null;
        });
    }

    private boolean isOutsideBorder(World world, int x, int z) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();

        double radius = border.getSize() / 2.0;

        return Math.abs(x - center.getBlockX()) > radius || Math.abs(z - center.getBlockZ()) > radius;
    }

    private Location resolveCandidate(World world, WorldRTPSettings settings, int x, int z) {
        // normal worlds ez
        if (world.getEnvironment() != World.Environment.NETHER) {
            int y = world.getHighestBlockYAt(x, z);
            if (y <= 0)
                return null;

            y = Math.max(minY, Math.min(y, maxY));

            Biome biome = world.getBiome(x, y, z);
            String biomeName = biome.name().toLowerCase();
            if (settings.blockedBiomes().contains(biomeName))

                return null;

            Location candidate = new Location(world, x + 0.5, y + 1, z + 0.5);

            return isSafeLocation(candidate) ? candidate : null;
        }

        // nether pain
        Biome biome = world.getBiome(x, 64, z);
        String biomeName = biome.name().toLowerCase();
        if (settings.blockedBiomes().contains(biomeName))

            return null;

        for (int[] range : netherSearchRanges) {
            int rangeMin = Math.max(range[0], minY);
            int rangeMax = Math.min(range[1], maxY);

            if (rangeMin >= rangeMax)
                continue;

            int y = findSafeNetherYInRange(world, x, z, rangeMin, rangeMax);

            if (y != -1) {
                Location candidate = new Location(world, x + 0.5, y, z + 0.5);

                if (isSafeNetherSpot(world, x, y, z)) {

                    return candidate;
                }
            }
        }

        return null;
    }

    // scan top down in range
    private int findSafeNetherYInRange(World world, int x, int z, int rangeMin, int rangeMax) {
        for (int y = rangeMax; y >= rangeMin; y--) {
            if (isSafeNetherSpot(world, x, y, z)) {
                return y;
            }
        }
        return -1;
    }

    // nether safety check, way stricter now
    private boolean isSafeNetherSpot(World world, int x, int y, int z) {
        if (y < 1 || y >= 127)

            return false;

        Material ground = world.getBlockAt(x, y - 1, z).getType();
        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();

        if (!ground.isSolid() || unsafeGround.contains(ground))
            return false;

        if (ground == Material.BEDROCK)
            return false;

        if (!isPassable(feet) || !isPassable(head))
            return false;

        if (unsafeNearby.contains(feet) || unsafeNearby.contains(head))
            return false;

        int unsafeNeighbors = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0)
                    continue;

                Material nearby = world.getBlockAt(x + dx, y - 1, z + dz).getType();
                if (unsafeNearby.contains(nearby)) {
                    unsafeNeighbors++;
                }
                if (unsafeNeighbors > 3)

                    return false;
            }
        }

        for (int dy = 1; dy <= 3; dy++) {
            Material below = world.getBlockAt(x, y - 1 - dy, z).getType();
            if (below == Material.LAVA) {
                if (dy <= 2)

                    return false;
            }
        }

        return true;
    }

    private boolean isPassable(Material mat) {
        return passableBlocks.contains(mat) || mat.isAir() || (!mat.isSolid() && !unsafeGround.contains(mat));
    }

    // normal world safety
    private boolean isSafeLocation(Location location) {
        World world = location.getWorld();

        int x = location.getBlockX();
        int y = location.getBlockY();

        int z = location.getBlockZ();

        Material ground = world.getBlockAt(x, y - 1, z).getType();

        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();

        if (!ground.isSolid() || unsafeGround.contains(ground))
            return false;

        if (unsafeGround.contains(feet) || unsafeGround.contains(head))
            return false;

        boolean passableFeet = isPassable(feet);
        boolean passableHead = isPassable(head);


        return passableFeet && passableHead;

    }

    // particles
    private void spawnTeleportParticles(Location loc) {
        World world = loc.getWorld();
        for (int i = 0; i < 30; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 2;
            double offsetY = random.nextDouble() * 2;
            double offsetZ = (random.nextDouble() - 0.5) * 2;
            world.spawnParticle(Particle.PORTAL, loc.clone().add(offsetX, offsetY, offsetZ), 1, 0, 0, 0, 0.1);
        }
        world.spawnParticle(Particle.EXPLOSION, loc, 1);
    }

    // how many ppl in world
    public int getPlayerCountInWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);

        return world != null ? world.getPlayers().size() : 0;
    }

    public void cancelRtp(Player player) {
        cleanupWarmup(player);
    }

    public boolean isUseBorder() {
        return useBorder;
    }

    public long getCooldown() {
        return cooldown;
    }

    public long getWarmup() {
        return warmup;
    }

    public boolean isCancelOnMovement() {
        return cancelOnMovement;
    }

    public boolean isParticles() {
        return particles;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public record WorldRTPSettings(int minRadius, int maxRadius, List<String> blockedBiomes, boolean enabled,
                                   String displayName) {
        //
    }

    public record SearchResult(Location location, int attempts) {
        //
    }
}