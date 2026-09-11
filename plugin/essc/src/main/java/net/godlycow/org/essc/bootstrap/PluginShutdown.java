package net.godlycow.org.essc.bootstrap;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.APIProvider;

public final class PluginShutdown {

    private final EssentialsC plugin;

    public PluginShutdown(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public void stop() {
        shutdownManagers();
        runBackup();
        unregisterAPI();
        plugin.debug("EssentialsC disabled.");
    }

    private void shutdownManagers() {
        if (plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().shutdown();
        }
        if (plugin.getAfkManager() != null) {
            plugin.getAfkManager().shutdown();
        }
        if (plugin.getHomeManager() != null) {
            plugin.getHomeManager().shutdown();
        }
        if (plugin.getUserManager() != null) {
            plugin.getUserManager().shutdown();
        }
        if (plugin.getAuctionManager() != null) {
            plugin.getAuctionManager().shutdown();
        }
        if (plugin.getShopManager() != null) {
            plugin.getShopManager().shutdown();
        }
        if (plugin.getKitManager() != null) {
            plugin.getKitManager().shutdown();
        }
        if (plugin.getNickManager() != null) {
            plugin.getNickManager().shutdown();
        }
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().shutdown();
        }
        if (plugin.getBackManager() != null) {
            plugin.getBackManager().shutdown();
        }
        if (plugin.getDiscordSRVHook() != null) {
            plugin.getDiscordSRVHook().shutdown();
        }
        if (plugin.getRtpManager() != null) {
            plugin.getRtpManager().shutdown();
        }
        if (plugin.getRtpGuiManager() != null) {
            plugin.getRtpGuiManager().shutdown();
        }
        if (plugin.getWarpManager() != null) {
            plugin.getWarpManager().shutdown();
        }
        if (plugin.getPunishmentManager() != null) {
            plugin.getPunishmentManager().shutdown();
        }
        if (plugin.getFlyManager() != null) {
            plugin.getFlyManager().shutdown();
        }
        if (plugin.getTabManager() != null) {
            plugin.getTabManager().shutdown();
        }
        if (plugin.getChatManager() != null) {
            plugin.getChatManager().shutdown();
        }
        if (plugin.getSpawnManager() != null) {
            plugin.getSpawnManager().shutdown();
        }

        plugin.getFastStatsManager().shutdown();

        plugin.debug("All managers shut down.");
    }

    private void runBackup() {
        if (!plugin.getConfigManager().isBackupOnShutdown()) {
            return;
        }

        if (plugin.getBackupManager() == null) {
            plugin.getLogger().warning("[Backup] Backup manager is not initialized, skipping shutdown backup.");
            return;
        }

        plugin.debug("[Backup] Creating shutdown backup...");
        try {
            plugin.getBackupManager().createSync(
                    name -> plugin.debug("[Backup] Shutdown backup created: " + name),
                    err  -> plugin.getLogger().warning("[Backup] Shutdown backup failed: " + err)
            );
        } catch (Exception ex) {
            plugin.getLogger().warning("[Backup] Shutdown backup error: " + ex.getMessage());
        }
    }

    private void unregisterAPI() {
        if (plugin.getApiImplementation() != null) {
            APIProvider.unregister();
        }
    }
}