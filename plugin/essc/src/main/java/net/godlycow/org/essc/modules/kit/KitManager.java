package net.godlycow.org.essc.modules.kit;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.impl.kit.KitImpl;
import net.godlycow.org.essc.api.kit.event.KitCooldownExpireEvent;
import net.godlycow.org.essc.api.kit.event.KitFirstJoinEvent;
import net.godlycow.org.essc.api.kit.event.KitReloadEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class KitManager implements Listener {
    private final EssentialsC plugin;
    private final KitDefinitions definitions;
    private final KitData data;
    private final KitCooldowns cooldowns;
    private final KitPermissions permissions;
    private final KitClaims claims;
    private KitSyncHook networkHook = null;

    public KitManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.definitions = new KitDefinitions(plugin);
        this.data = new KitData(plugin);
        this.cooldowns = new KitCooldowns(plugin, data);
        this.permissions = new KitPermissions();
        this.claims = new KitClaims(plugin, data, cooldowns);

        data.connect();
        definitions.loadAll();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.debug("KitManager initialized");

        startCooldownNotificationTask();
    }

    private void startCooldownNotificationTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (Kit kit : definitions.getKits()) {
                    if (kit.getCooldown() > 0 && data.hasClaimed(player.getUniqueId(), kit.getName())) {
                        cooldowns.getRemainingSeconds(player, kit);
                    }
                }
            }
        }, 20L * 30, 20L * 30);
    }

    public void setNetworkHook(KitSyncHook hook) {
        this.networkHook = hook;
        plugin.debug("[KitManager] Network kit sync hook registered.");
    }

    public void clearNetworkHook() {
        this.networkHook = null;
        plugin.debug("[KitManager] Network kit sync hook cleared.");
    }

    public KitSyncHook getNetworkHook() {
        return networkHook;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        final boolean isFirstJoin = !player.hasPlayedBefore();

        CompletableFuture<Void> dataFuture = data.loadPlayerData(player.getUniqueId());
        CompletableFuture<Void> notifFuture = cooldowns.loadNotificationsEnabled(player.getUniqueId());

        CompletableFuture.allOf(dataFuture, notifFuture).thenRun(() -> {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                if (!player.isOnline()) return;

                if (isFirstJoin) {
                    plugin.debug("First join detected for " + player.getName() + ", checking first-join kits");

                    for (Kit kit : definitions.getKits()) {
                        if (kit.isFirstJoin()) {
                            KitImpl apiKit = new KitImpl(kit);
                            KitFirstJoinEvent firstJoinEvent = new KitFirstJoinEvent(player, apiKit);
                            Bukkit.getPluginManager().callEvent(firstJoinEvent);

                            if (!firstJoinEvent.isCancelled()) {
                                plugin.debug("Giving first-join kit: " + kit.getName());
                                giveKit(player, kit);
                            }
                        }
                    }
                }

                for (Kit kit : definitions.getKits()) {
                    if (kit.getCooldown() > 0 && data.hasClaimed(player.getUniqueId(), kit.getName())) {
                        cooldowns.getRemainingSeconds(player, kit);
                    }
                }
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        data.removePlayerCache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCooldownExpire(KitCooldownExpireEvent event) {
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;
        if (!cooldowns.isNotificationsEnabled(player.getUniqueId())) return;

        Kit kit = definitions.getKit(event.getKit().getName());
        if (kit == null) return;

        player.sendMessage(plugin.getLanguageManager().get(player, "kit.notification.available",
                Map.of("kit", kit.getDisplayName())));

        plugin.debug("Sent cooldown expiry notification to " + player.getName() + " for kit " + kit.getName());
    }

    public Kit getKit(String name) {
        return definitions.getKit(name);
    }

    public Collection<Kit> getKits() {
        return definitions.getKits();
    }

    public boolean hasPermission(Player player, Kit kit) {
        return permissions.hasPermission(player, kit);
    }

    public boolean hasCooldownBypass(Player player, Kit kit) {
        return permissions.hasCooldownBypass(player, kit);
    }

    public boolean canClaim(Player player, Kit kit) {
        return permissions.canClaim(player, kit, data);
    }

    public long getCooldownRemaining(Player player, Kit kit) {
        return cooldowns.getRemainingSeconds(player, kit);
    }

    public CompletableFuture<Long> getCooldownRemainingAsync(Player player, Kit kit) {
        long localRemaining = getCooldownRemaining(player, kit);

        if (!kit.isNetworkSync()
                || networkHook == null
                || player.hasPermission("essentialsc.kits.networksync.bypass")) {
            CompletableFuture<Long> future = new CompletableFuture<>();
            future.complete(localRemaining);
            return future;
        }

        return networkHook.getNetworkLastClaimed(player.getUniqueId(), kit.getName())
                .thenApply(networkLastClaimed -> {
                    if (networkLastClaimed == 0) {
                        return localRemaining;
                    }

                    long networkCooldownEnd = networkLastClaimed + (kit.getCooldown() * 1000L);
                    long networkRemaining = Math.max(0,
                            (networkCooldownEnd - System.currentTimeMillis()) / 1000L);

                    return Math.max(localRemaining, networkRemaining);
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("[KitManager] Network cooldown check failed for "
                            + player.getName() + "/" + kit.getName() + ": " + ex.getMessage());
                    return localRemaining;
                });
    }

    public boolean hasClaimed(Player player, Kit kit) {
        return data.hasClaimed(player.getUniqueId(), kit.getName());
    }

    public int getClaimCount(Player player, Kit kit) {
        return data.getClaimCount(player.getUniqueId(), kit.getName());
    }

    public long getLastClaimedTimestamp(Player player, Kit kit) {
        return data.getLastClaimed(player.getUniqueId(), kit.getName());
    }

    public void giveKit(Player player, Kit kit) {
        claims.execute(player, kit);
    }

    public boolean isNotificationsEnabled(UUID uuid) {
        return cooldowns.isNotificationsEnabled(uuid);
    }

    public void setNotificationsEnabled(UUID uuid, boolean enabled) {
        cooldowns.setNotificationsEnabled(uuid, enabled);
    }

    public void reload() {
        definitions.loadAll();
        data.clearCache();
        cooldowns.clearAllNotifications();

        for (Player p : Bukkit.getOnlinePlayers()) {
            data.loadPlayerData(p.getUniqueId());
            cooldowns.loadNotificationsEnabled(p.getUniqueId());
        }

        KitReloadEvent reloadEvent = new KitReloadEvent(definitions.getKitCount(), System.currentTimeMillis());
        Bukkit.getPluginManager().callEvent(reloadEvent);

        plugin.debug("Kit configuration reloaded");
    }

    public void shutdown() {
        data.disconnect();
    }
}
