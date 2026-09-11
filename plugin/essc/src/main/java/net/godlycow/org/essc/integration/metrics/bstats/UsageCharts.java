package net.godlycow.org.essc.integration.metrics.bstats;

import net.godlycow.org.essc.EssentialsC;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UsageCharts {

    private UsageCharts() {}

    public static void register(EssentialsC plugin, Metrics metrics) {
        registerDefaultLanguageChart(plugin, metrics);
        registerDisabledCommandsChart(plugin, metrics);
        registerLuckPermsIntegrationChart(plugin, metrics);
        registerDiscordSRVChart(plugin, metrics);
    }

    private static void registerDefaultLanguageChart(EssentialsC plugin, Metrics metrics) {
        metrics.addCustomChart(new SimplePie("default_language", () -> {
            String lang = plugin.getConfigManager().getDefaultLanguage();
            return lang != null ? lang : "en_US";
        }));
    }



    private static void registerDisabledCommandsChart(EssentialsC plugin, Metrics metrics) {
        metrics.addCustomChart(new SimplePie("disabled_commands_count", () -> {
            Set<String> keys = plugin.getCommandsConfig().getConfig().getKeys(false);
            long disabledCount = keys.stream()
                    .filter(key -> !plugin.getCommandsConfig().isEnabled(key))
                    .count();
            if (disabledCount == 0) return "None";
            if (disabledCount <= 5) return "1-5";
            if (disabledCount <= 15) return "6-15";
            return "16+";
        }));
    }

    private static void registerLuckPermsIntegrationChart(EssentialsC plugin, Metrics metrics) {
        metrics.addCustomChart(new AdvancedPie("luckperms_integrations", () -> {
            Map<String, Integer> integrations = new HashMap<>();
            if (plugin.getConfigManager().isLuckPermsChatEnabled()) {
                integrations.put("Chat formatting", 1);
            }
            if (plugin.getConfigManager().isLuckPermsTabEnabled()) {
                integrations.put("Tab formatting", 1);
            }
            if (integrations.isEmpty()) {
                integrations.put("None", 1);
            }
            return integrations;
        }));
    }


    private static void registerDiscordSRVChart(EssentialsC plugin, Metrics metrics) {
        metrics.addCustomChart(new SimplePie("discordsrv_enabled", () -> {
            return plugin.getConfigManager().isDiscordSRVEnabled() ? "Enabled" : "Disabled";
        }));
    }

}