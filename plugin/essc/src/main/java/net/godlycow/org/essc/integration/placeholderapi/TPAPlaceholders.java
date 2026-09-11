package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.teleport.TPAManager;
import org.bukkit.entity.Player;

import java.util.List;

public class TPAPlaceholders {

    private final EssentialsC plugin;

    public TPAPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("tpa_")) {
            return null;
        }

        TPAManager manager = plugin.getTPAManager();
        if (manager == null) {
            return "";
        }

        return switch (identifier.toLowerCase()) {
            case "tpa_pending_incoming" -> String.valueOf(manager.getIncomingRequests(player).size());
            case "tpa_pending_outgoing" -> String.valueOf(manager.getOutgoingRequests(player).size());
            case "tpa_has_incoming" -> String.valueOf(manager.hasIncomingRequests(player));
            case "tpa_has_outgoing" -> String.valueOf(manager.hasOutgoingRequests(player));
            case "tpa_blocked" -> String.valueOf(manager.getBlockedPlayers().contains(player.getUniqueId()));
            default -> null;
        };
    }

    public static List<String> getPlaceholderList() {
        return List.of(
                "%essc_tpa_pending_incoming% — number of incoming TPA requests",
                "%essc_tpa_pending_outgoing% — number of outgoing TPA requests",
                "%essc_tpa_has_incoming%     — 'true' if player has incoming requests",
                "%essc_tpa_has_outgoing%     — 'true' if player has outgoing requests",
                "%essc_tpa_blocked%          — 'true' if player has TPA toggled off"
        );
    }
}