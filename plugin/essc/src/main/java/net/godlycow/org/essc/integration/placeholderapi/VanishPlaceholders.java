package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class VanishPlaceholders {

    private final EssentialsC plugin;

    public VanishPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("vanish_")) {
            return null;
        }

        boolean isVanished = plugin.getVanishManager().isVanished(player);

        return switch (identifier.toLowerCase()) {
            case "vanish_status" -> isVanished ? "Vanished" : "Visible";
            case "vanish_boolean" -> isVanished ? "true" : "false";
            default -> null;
        };
    }

    public static List<String> getPlaceholderList() {
        List<String> list = new ArrayList<>();

        list.add("%essc_vanish_status% - Returns 'Vanished' or 'Visible'");
        list.add("%essc_vanish_boolean% - Returns 'true' or 'false'");

        return list;
    }
}