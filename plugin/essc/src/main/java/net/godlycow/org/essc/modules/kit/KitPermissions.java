package net.godlycow.org.essc.modules.kit;

import net.godlycow.org.essc.api.impl.kit.KitImpl;
import net.godlycow.org.essc.api.kit.event.KitAvailableCheckEvent;
import net.godlycow.org.essc.api.kit.event.KitPermissionCheckEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class KitPermissions {

    public boolean hasPermission(Player player, Kit kit) {
        boolean basePermission = player.hasPermission(kit.getPermission()) || player.hasPermission("essentialsc.kits.admin");

        KitImpl apiKit = new KitImpl(kit);
        KitPermissionCheckEvent event = new KitPermissionCheckEvent(player, apiKit, basePermission);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        return event.hasPermission();
    }

    public boolean canClaim(Player player, Kit kit, KitData data) {
        if (!hasPermission(player, kit)) {
            KitImpl apiKit = new KitImpl(kit);
            KitAvailableCheckEvent event = new KitAvailableCheckEvent(player, apiKit, false, "no_permission");
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return false;
            }

            return event.isAvailable();
        }

        java.util.Map<String, PlayerKitData> playerData = data.getPlayerData(player.getUniqueId());
        PlayerKitData claimData = playerData.get(kit.getName());

        if (kit.isOneTime()) {
            if (claimData != null && claimData.claimCount > 0) {
                KitImpl apiKit = new KitImpl(kit);
                KitAvailableCheckEvent event = new KitAvailableCheckEvent(player, apiKit, false, "one_time_used");
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return false;
                }

                return event.isAvailable();
            }
        }

        if (kit.getMaxClaims() > 0) {
            if (claimData != null && claimData.claimCount >= kit.getMaxClaims()) {
                KitImpl apiKit = new KitImpl(kit);
                KitAvailableCheckEvent event = new KitAvailableCheckEvent(player, apiKit, false, "max_claims_reached");
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return false;
                }

                return event.isAvailable();
            }
        }

        if (kit.getCooldown() > 0 && !hasCooldownBypass(player, kit)) {
            if (claimData != null) {
                long cooldownEnd = claimData.lastClaimed + (kit.getCooldown() * 1000L);
                if (System.currentTimeMillis() < cooldownEnd) {
                    KitImpl apiKit = new KitImpl(kit);
                    KitAvailableCheckEvent event = new KitAvailableCheckEvent(player, apiKit, false, "cooldown_active");
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return false;
                    }

                    return event.isAvailable();
                }
            }
        }

        KitImpl apiKit = new KitImpl(kit);
        KitAvailableCheckEvent event = new KitAvailableCheckEvent(player, apiKit, true, "");
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        return event.isAvailable();
    }

    public boolean hasCooldownBypass(Player player, Kit kit) {
        String perKitPermission = "essentialsc.kits.cooldown.bypass." + kit.getName().toLowerCase();
        return player.hasPermission("essentialsc.kits.admin")
                || player.hasPermission("essentialsc.kits.cooldown.bypass")
                || player.hasPermission(perKitPermission);
    }
}