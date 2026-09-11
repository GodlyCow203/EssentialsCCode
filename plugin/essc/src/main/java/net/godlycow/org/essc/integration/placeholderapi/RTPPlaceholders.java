package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.rtp.RTPManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RTPPlaceholders {

    private final EssentialsC plugin;

    public RTPPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("rtp_")) {
            return null;
        }

        RTPManager manager = plugin.getRtpManager();
        if (manager == null) return null;

        return switch (identifier.toLowerCase()) {
            case "rtp_cooldown" -> {
                if (player == null) yield "0";
                yield String.valueOf(manager.getRemainingCooldown(player));
            }
            case "rtp_cooldown_formatted" -> {
                if (player == null) yield "0s";
                long seconds = manager.getRemainingCooldown(player);
                if (seconds <= 0) yield "0s";
                long mins = seconds / 60;
                long secs = seconds % 60;
                if (mins > 0) yield mins + "m " + secs + "s";
                yield secs + "s";
            }
            case "rtp_in_progress" -> {
                if (player == null) yield "false";
                yield manager.isRtpInProgress(player) ? "true" : "false";
            }
            case "rtp_world_overworld" -> String.valueOf(manager.getPlayerCountInWorld("world"));
            case "rtp_world_nether" -> String.valueOf(manager.getPlayerCountInWorld("world_nether"));
            case "rtp_world_end" -> String.valueOf(manager.getPlayerCountInWorld("world_the_end"));
            default -> {
                if (identifier.toLowerCase().startsWith("rtp_world_")) {
                    String worldName = identifier.substring(10);
                    yield String.valueOf(manager.getPlayerCountInWorld(worldName));
                }
                yield null;
            }
        };
    }

    public static List<String> getPlaceholderList() {
        List<String> list = new ArrayList<>();

        list.add("%essc_rtp_cooldown% - Returns remaining cooldown in seconds");
        list.add("%essc_rtp_cooldown_formatted% - Returns formatted cooldown (e.g., '2m 30s')");
        list.add("%essc_rtp_in_progress% - Returns 'true' if player has RTP in progress");
        list.add("%essc_rtp_world_overworld% - Returns player count in overworld");
        list.add("%essc_rtp_world_nether% - Returns player count in nether");
        list.add("%essc_rtp_world_end% - Returns player count in end");
        list.add("%essc_rtp_world_<worldname>% - Returns player count in specified world");

        return list;
    }
}