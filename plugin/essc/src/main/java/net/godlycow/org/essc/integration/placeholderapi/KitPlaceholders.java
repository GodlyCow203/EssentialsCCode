package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.kit.Kit;
import net.godlycow.org.essc.modules.kit.KitManager;
import org.bukkit.entity.Player;

import java.util.List;

public class KitPlaceholders {

    private final EssentialsC plugin;

    public KitPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("kit_")) {
            return null;
        }

        KitManager manager = plugin.getKitManager();
        if (manager == null) {
            return "";
        }

        if (identifier.toLowerCase().startsWith("kit_cooldown_")) {
            String kitName = identifier.substring("kit_cooldown_".length());
            Kit kit = manager.getKit(kitName);
            if (kit == null) {
                return "0";
            }
            return String.valueOf(manager.getCooldownRemaining(player, kit));
        }

        if (identifier.toLowerCase().startsWith("kit_cooldown_formatted_")) {
            String kitName = identifier.substring("kit_cooldown_formatted_".length());
            Kit kit = manager.getKit(kitName);
            if (kit == null) {
                return "0s";
            }
            return formatSeconds(manager.getCooldownRemaining(player, kit));
        }

        if (identifier.toLowerCase().startsWith("kit_claimed_")) {
            String kitName = identifier.substring("kit_claimed_".length());
            Kit kit = manager.getKit(kitName);
            if (kit == null) {
                return "false";
            }
            return String.valueOf(manager.hasClaimed(player, kit));
        }

        if (identifier.toLowerCase().startsWith("kit_claims_")) {
            String kitName = identifier.substring("kit_claims_".length());
            Kit kit = manager.getKit(kitName);
            if (kit == null) {
                return "0";
            }
            return String.valueOf(manager.getClaimCount(player, kit));
        }

        if (identifier.toLowerCase().startsWith("kit_has_")) {
            String kitName = identifier.substring("kit_has_".length());
            Kit kit = manager.getKit(kitName);
            if (kit == null) {
                return "false";
            }
            return String.valueOf(manager.hasPermission(player, kit));
        }

        return switch (identifier.toLowerCase()) {
            case "kit_count" -> String.valueOf(manager.getKits().size());
            default -> null;
        };
    }

    private String formatSeconds(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        long hours = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return hours + "h " + mins + "m " + secs + "s";
        }
        if (mins > 0) {
            return mins + "m " + secs + "s";
        }
        return secs + "s";
    }

    public static List<String> getPlaceholderList() {
        return List.of(
                "%essc_kit_count%                    — total number of kits defined",
                "%essc_kit_cooldown_<n>%          — remaining cooldown for a kit in seconds",
                "%essc_kit_cooldown_formatted_<n>% — formatted cooldown e.g. '1h 2m 30s'",
                "%essc_kit_claimed_<n>%           — 'true' if player has ever claimed the kit",
                "%essc_kit_claims_<n>%            — number of times player claimed the kit",
                "%essc_kit_has_<n>%              — 'true' if player has permission for the kit"
        );
    }
}