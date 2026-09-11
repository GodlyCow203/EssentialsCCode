package net.godlycow.org.essc.modules;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager implements Listener {
    private final EssentialsC plugin;
    private final Set<UUID> vanishedPlayers;

    private boolean hideFromTab;
    private boolean giveNightVision;
    private boolean preventMobTarget;
    private boolean disableCollisions;

    public VanishManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.vanishedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.debug("Vanish Manager initialized");
    }

    public void loadConfig() {
        this.hideFromTab = plugin.getConfigManager().isVanishHideFromTab();
        this.giveNightVision = plugin.getConfigManager().isVanishNightVision();
        this.preventMobTarget = plugin.getConfigManager().isVanishPreventMobTarget();
        this.disableCollisions = plugin.getConfigManager().isVanishDisableCollisions();
    }

    public void vanish(Player player) {
        vanishedPlayers.add(player.getUniqueId());
        player.setMetadata("vanished", new FixedMetadataValue(plugin, true));

        if (plugin.getUserManager() != null) {
            plugin.getUserManager().setVanished(player.getUniqueId(), true);
        }

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(player)) {
                if (online.hasPermission("essentialsc.vanish.see")) {
                    online.showPlayer(plugin, player);
                } else {
                    online.hidePlayer(plugin, player);
                }
            }
        }

        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        if (giveNightVision) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        }

        if (preventMobTarget) player.setAffectsSpawning(false);
        if (disableCollisions) player.setCollidable(false);

        updateTabForAll();

        plugin.debug(player.getName() + " is now vanished");
    }

    public void unvanish(Player player) {
        vanishedPlayers.remove(player.getUniqueId());
        player.removeMetadata("vanished", plugin);

        if (plugin.getUserManager() != null) {
            plugin.getUserManager().setVanished(player.getUniqueId(), false);
        }

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.showPlayer(plugin, player);
            }
        }

        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        if (giveNightVision) player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        if (preventMobTarget) player.setAffectsSpawning(true);
        if (disableCollisions) player.setCollidable(true);

        updateTabForAll();

        plugin.debug(player.getName() + " is no longer vanished");
    }

    private void updateTabForAll() {
        if (plugin.getTabManager() != null) {
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                plugin.getTabManager().updatePlayerTab(online);
            }
        }
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public Set<UUID> getVanishedPlayers() {
        return Collections.unmodifiableSet(vanishedPlayers);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();

        if (joining.hasPermission("essentialsc.vanish.onjoin")) {
            vanishedPlayers.add(joining.getUniqueId());
        }

        if (plugin.getUserManager() != null && plugin.getUserManager().isVanished(joining.getUniqueId())) {
            vanishedPlayers.add(joining.getUniqueId());
        }

        if (vanishedPlayers.contains(joining.getUniqueId())) {
            joining.setMetadata("vanished", new FixedMetadataValue(plugin, true));
            joining.getScheduler().runDelayed(plugin, task -> vanish(joining), null, 2L);
        }

        for (UUID vanishedId : vanishedPlayers) {
            Player vanished = plugin.getServer().getPlayer(vanishedId);
            if (vanished != null && !vanished.equals(joining)) {
                if (joining.hasPermission("essentialsc.vanish.see")) {
                    joining.showPlayer(plugin, vanished);
                } else {
                    joining.hidePlayer(plugin, vanished);
                }
            }
        }
    }
}