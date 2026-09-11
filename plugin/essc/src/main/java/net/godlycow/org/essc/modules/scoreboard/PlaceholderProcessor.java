package net.godlycow.org.essc.modules.scoreboard;

import me.clip.placeholderapi.PlaceholderAPI;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.plugin.economy.EconomyManager;
import net.godlycow.org.essc.util.LegacyColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class PlaceholderProcessor {

    private final boolean papiEnabled;
    private final EssentialsC plugin;

    public PlaceholderProcessor(EssentialsC plugin) {
        this.plugin = plugin;
        this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public boolean isPapiEnabled() {
        return papiEnabled;
    }

    public String processString(Player player, String text) {
        if (text == null || text.isEmpty()) return "";

        String processed = text;
        if (papiEnabled) {
            try {
                processed = PlaceholderAPI.setPlaceholders(player, processed);
            } catch (Exception e) {
                Bukkit.getLogger().warning("PAPI failed for " + player.getName() + ": " + e.getMessage());
            }
        }

        processed = processBuiltInPlaceholders(player, processed);

        return LegacyColorConverter.toMiniMessage(processed);
    }

    private String processBuiltInPlaceholders(Player player, String text) {
        String processed = text;

        processed = processed
                .replace("%player_name%",        player.getName())
                .replace("%player_displayname%", player.getDisplayName())
                .replace("%server_online%",      String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%server_max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%world%",              player.getWorld().getName())
                .replace("%x%",                  String.valueOf(player.getLocation().getBlockX()))
                .replace("%y%",                  String.valueOf(player.getLocation().getBlockY()))
                .replace("%z%",                  String.valueOf(player.getLocation().getBlockZ()))
                .replace("%ping%",               String.valueOf(player.getPing()))
                .replace("%player_ping%",        String.valueOf(player.getPing()))
                .replace("%level%",              String.valueOf(player.getLevel()));

        processed = processed  //more fallback placeholders to complete missing papi ones
                .replace("%statistic_player_kills%", String.valueOf(player.getStatistic(org.bukkit.Statistic.PLAYER_KILLS)))
                .replace("%statistic_deaths%",       String.valueOf(player.getStatistic(org.bukkit.Statistic.DEATHS)));

        processed = processed
                .replace("%server_tps_1%",     String.format("%.1f", Bukkit.getServer().getTPS()[0]))
                .replace("%server_tps_5%",     String.format("%.1f", Bukkit.getServer().getTPS()[1]))
                .replace("%server_tps_15%",    String.format("%.1f", Bukkit.getServer().getTPS()[2]))
                .replace("%server_version%",   Bukkit.getBukkitVersion());

        if (processed.contains("%luckperms_prefix%") || processed.contains("%luckperms_suffix%")) {
            String luckpermsPrefix = getLuckPermsPrefix(player);
            String luckpermsSuffix = getLuckPermsSuffix(player);
            processed = processed
                    .replace("%luckperms_prefix%", luckpermsPrefix)
                    .replace("%luckperms_suffix%", luckpermsSuffix);
        }

        if (processed.contains("%vault_eco_balance%") || processed.contains("%vault_eco_balance_formatted%")) {
            BigDecimal balance = getBalance(player);
            String raw = balance != null ? balance.toPlainString() : "0";
            String formatted = getVaultFormatted(player, balance);
            processed = processed
                    .replace("%vault_eco_balance%", raw)
                    .replace("%vault_eco_balance_formatted%", formatted);
        }

        return processed;
    }
    private String getLuckPermsPrefix(Player player) {
        try {
            //return empty prefix as fallback if luckp is null
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
                return "";
            }

            //get players metadata from luckp
            LuckPerms api = LuckPermsProvider.get();
            CachedMetaData metaData = api.getPlayerAdapter(Player.class).getMetaData(player);

            return metaData.getPrefix() != null ? metaData.getPrefix() : "";

        } catch (Exception ignored) {
            // Silent falback
        }

        return "";
    }


    //same thing but for suffix
    private String getLuckPermsSuffix(Player player) {
        try {
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
                return "";
            }
            LuckPerms api = LuckPermsProvider.get();
            CachedMetaData metaData = api.getPlayerAdapter(Player.class).getMetaData(player);

            return metaData.getSuffix() != null ? metaData.getSuffix() : "";
        } catch (Exception ignored) {
            // silent fallback
        }
        return "";
    }

    private BigDecimal getBalance(Player player) {
        //use essentialscs economy because this is the same
        // manager Vault is reading from
        try {
            if (plugin.getEconomyManager() != null) {
                return plugin.getEconomyManager().getCachedBalance(player.getUniqueId());
            }
        } catch (Exception ignored) {
        }
        return BigDecimal.ZERO;
    }

    private String getVaultFormatted(Player player, BigDecimal balance) {
        //Vaults own formatter
        if (plugin.isVaultHooked()) {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                var rsp = Bukkit.getServicesManager().getRegistration(economyClass);
                if (rsp != null) {
                    Object eco = rsp.getProvider();
                    return (String) eco.getClass().getMethod("format", double.class).invoke(eco, balance.doubleValue());
                }
            } catch (Exception ignored) {
            }
        }
        //fallback, format with essentialscs own currency formatter
        try {
            if (plugin.getEconomyManager() != null) {
                return plugin.getEconomyManager().format(balance);
            }
        } catch (Exception ignored)
        {
        }

        return balance == null ? "0" : balance.toPlainString();
    }
}