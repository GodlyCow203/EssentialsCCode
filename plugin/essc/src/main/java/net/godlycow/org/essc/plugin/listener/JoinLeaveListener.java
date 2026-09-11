package net.godlycow.org.essc.plugin.listener;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.InventorySerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class JoinLeaveListener implements Listener {

    private final EssentialsC plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final boolean placeholderAPIEnabled;

    public JoinLeaveListener(EssentialsC plugin) {
        this.plugin = plugin;
        this.placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        if (plugin.getUserManager() != null) {
            InvseeListener invseeListener = plugin.getInvseeListener();

            if (invseeListener != null && invseeListener.hasOpenOfflineSession(joiningPlayer.getUniqueId())) {
                invseeListener.closeSessionsForPlayer(joiningPlayer.getUniqueId(), joiningPlayer);
            } else {
                plugin.getUserManager().loadInventory(joiningPlayer.getUniqueId()).thenAccept(base64 -> {
                    if (base64 == null) return;
                    plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                        if (!joiningPlayer.isOnline()) return;
                        ItemStack[] slots = InventorySerializer.deserialize(base64);
                        InventorySerializer.applyToInventory(slots, joiningPlayer.getInventory());
                        joiningPlayer.updateInventory();
                        plugin.debug("Restored modified offline inventory for " + joiningPlayer.getName());
                    });
                    plugin.getUserManager().deleteInventory(joiningPlayer.getUniqueId());
                });
            }
        }

        if (!plugin.getConfigManager().isJoinLeaveEnabled()) {
            event.setJoinMessage(null);
            return;
        }

        boolean hideVanished = plugin.getConfigManager().isJoinLeaveHideVanished();
        boolean isFirstJoin = !event.getPlayer().hasPlayedBefore();
        String message;

        if (isFirstJoin) {
            message = plugin.getConfigManager().getFirstJoinMessage();
        } else {
            message = plugin.getConfigManager().getJoinMessage();
        }

        if (message == null || message.isEmpty()) {
            return;
        }

        Component componentMessage = formatMessage(message, event.getPlayer());

        event.setJoinMessage(null);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hideVanished && plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(event.getPlayer()) && !player.hasPermission("essentialsc.vanish.see")) {
                continue;
            }
            player.sendMessage(componentMessage);
        }

        Bukkit.getConsoleSender().sendMessage(componentMessage);
        plugin.debug("Custom join message sent for " + event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quittingPlayer = event.getPlayer();

        if (plugin.getUserManager() != null) {
            String base64 = InventorySerializer.serialize(quittingPlayer.getInventory());
            plugin.getUserManager().saveInventory(quittingPlayer.getUniqueId(), base64);
        }

        if (!plugin.getConfigManager().isJoinLeaveEnabled()) {
            event.setQuitMessage(null);
            return;
        }

        boolean hideVanished = plugin.getConfigManager().isJoinLeaveHideVanished();
        String message = plugin.getConfigManager().getLeaveMessage();

        if (message == null || message.isEmpty()) {
            return;
        }

        Component componentMessage = formatMessage(message, event.getPlayer());

        event.setQuitMessage(null);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hideVanished && plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(event.getPlayer()) && !player.hasPermission("essentialsc.vanish.see")) {
                continue;
            }
            player.sendMessage(componentMessage);
        }

        Bukkit.getConsoleSender().sendMessage(componentMessage);
        plugin.debug("Custom leave message sent for " + event.getPlayer().getName());
    }

    private Component formatMessage(String message, Player player) {
        String formattedMessage = message;

        if (placeholderAPIEnabled) {
            try {
                formattedMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, formattedMessage);
            } catch (Exception e) {
                plugin.debug("PlaceholderAPI processing failed for " + player.getName() + ": " + e.getMessage());
            }
        }

        formattedMessage = formattedMessage.replace("<player>", player.getName());

        return miniMessage.deserialize(formattedMessage);
    }

    public void reload() {
        plugin.getConfigManager().reload();
        plugin.debug("Join/Leave messages reloaded.");
    }
}