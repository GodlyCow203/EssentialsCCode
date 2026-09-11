package net.godlycow.org.essc.integration.bedrock;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class BedrockUtil {

    private final EssentialsC plugin;
    private final FloodgateHook floodgateHook;

    public BedrockUtil(EssentialsC plugin, FloodgateHook floodgateHook) {
        this.plugin = plugin;
        this.floodgateHook = floodgateHook;
    }

    public Player resolvePlayer(String name) {
        if (name == null || name.isBlank()) return null;

        Player exact = plugin.getServer().getPlayerExact(name);
        if (exact != null) return exact;

        if (floodgateHook.isAvailable()) {
            String prefixed = floodgateHook.getPrefix() + name;
            Player bedrock = plugin.getServer().getPlayerExact(prefixed);
            if (bedrock != null) {
                plugin.debug("BedrockUtil: resolved \"" + name + "\" to Bedrock player \"" + prefixed + "\"");
                return bedrock;
            }
        }

        return null;
    }

    public OfflinePlayer resolveOfflinePlayer(String name) {
        if (name == null || name.isBlank()) return null;

        Player online = resolvePlayer(name);
        if (online != null) return online;

        OfflinePlayer found = searchOffline(name);
        if (found != null) return found;

        if (floodgateHook.isAvailable()) {
            String prefixed = floodgateHook.getPrefix() + name;
            OfflinePlayer bedrockFound = searchOffline(prefixed);
            if (bedrockFound != null) {
                plugin.debug("BedrockUtil: resolved offline \"" + name + "\" to Bedrock player \"" + prefixed + "\"");
                return bedrockFound;
            }
        }

        return null;
    }
    public boolean isBedrockPlayer(Player player) {
        return floodgateHook.isBedrockPlayer(player);
    }

    private OfflinePlayer searchOffline(String name) {
        return Arrays.stream(plugin.getServer().getOfflinePlayers())
                .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(name))
                .filter(OfflinePlayer::hasPlayedBefore)
                .findFirst()
                .orElse(null);
    }
}
