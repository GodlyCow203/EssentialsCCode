package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.auction.AuctionManager;
import org.bukkit.entity.Player;

import java.util.List;

public class AuctionPlaceholders {

    private final EssentialsC plugin;

    public AuctionPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("ah_")) {
            return null;
        }

        AuctionManager manager = plugin.getAuctionManager();
        if (manager == null) {
            return "";
        }

        return switch (identifier.toLowerCase()) {
            case "ah_active_total" -> String.valueOf(manager.getActiveAuctions().size());
            case "ah_active_own" -> String.valueOf(manager.getPlayerAuctions(player.getUniqueId()).size());
            case "ah_has_expired" -> String.valueOf(manager.hasExpiredItems(player.getUniqueId()));
            case "ah_expired_count" -> String.valueOf(manager.getExpiredItems(player.getUniqueId()).size());
            case "ah_sell_history_count" -> String.valueOf(manager.getSellHistory(player.getUniqueId()).size());
            case "ah_buy_history_count" -> String.valueOf(manager.getBuyHistory(player.getUniqueId()).size());
            default -> null;
        };
    }

    public static List<String> getPlaceholderList() {
        return List.of(
                "%essc_ah_active_total%      — total active auctions on the server",
                "%essc_ah_active_own%        — number of player's own active listings",
                "%essc_ah_has_expired%       — 'true' if player has expired items to claim",
                "%essc_ah_expired_count%     — number of expired items waiting to be claimed",
                "%essc_ah_sell_history_count% — number of entries in player's sell history",
                "%essc_ah_buy_history_count%  — number of entries in player's buy history"
        );
    }
}