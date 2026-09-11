package net.godlycow.org.essc.modules.kit;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.impl.kit.KitImpl;
import net.godlycow.org.essc.api.kit.event.KitClaimEvent;
import net.godlycow.org.essc.api.kit.event.KitGiveEvent;
import net.godlycow.org.essc.api.kit.event.KitPostClaimEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KitClaims {
    private final EssentialsC plugin;
    private final KitData data;
    private final KitCooldowns cooldowns;

    public KitClaims(EssentialsC plugin, KitData data, KitCooldowns cooldowns) {
        this.plugin = plugin;
        this.data = data;
        this.cooldowns = cooldowns;
    }

    public void execute(Player player, Kit kit) {
        KitImpl apiKit = new KitImpl(kit);

        KitClaimEvent claimEvent = new KitClaimEvent(player, apiKit);
        Bukkit.getPluginManager().callEvent(claimEvent);

        if (claimEvent.isCancelled()) {
            return;
        }

        List<ItemStack> itemsToGive = new ArrayList<>(kit.getItems());
        KitGiveEvent giveEvent = new KitGiveEvent(player, apiKit, itemsToGive);
        Bukkit.getPluginManager().callEvent(giveEvent);

        if (giveEvent.isCancelled()) {
            return;
        }

        List<ItemStack> finalItems = giveEvent.getItems();

        for (ItemStack item : finalItems) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());

            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        long now = System.currentTimeMillis();

        data.recordClaim(player.getUniqueId(), kit.getName(), now).thenRun(() -> {
            cooldowns.clearNotification(player, kit);

            if (kit.isNetworkSync() && plugin.getKitManager().getNetworkHook() != null) {
                plugin.getKitManager().getNetworkHook().onKitClaimed(
                        player.getUniqueId(),
                        kit.getName(),
                        now,
                        plugin.getServer().getMotd()
                );
            }

            KitPostClaimEvent postEvent = new KitPostClaimEvent(player, apiKit, now);
            Bukkit.getPluginManager().callEvent(postEvent);
        });

        player.sendMessage(plugin.getLanguageManager().get(player, "kit.claim.success",
                Map.of("kit", kit.getDisplayName())));

        if (plugin.getDiscordSRVHook() != null && plugin.getDiscordSRVHook().isHooked()) {
            plugin.getDiscordSRVHook().sendKitClaimEmbed(
                    player.getUniqueId(),
                    player.getName(),
                    kit
            );
        }

        plugin.debug("Player " + player.getName() + " claimed kit " + kit.getName());
    }
}