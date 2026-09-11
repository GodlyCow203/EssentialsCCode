package net.godlycow.org.essc.migration.saver;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.migration.mapper.UserDataMapper.BanTransfer;
import net.godlycow.org.essc.migration.mapper.UserDataMapper.MuteTransfer;

import java.util.concurrent.CompletableFuture;

public class Punishment {
    private final EssentialsC plugin;

    public Punishment(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<PunishmentResult> writeMute(MuteTransfer data, boolean dryRun) {
        if (plugin.getPunishmentManager() == null) {
            return CompletableFuture.completedFuture(new PunishmentResult(false, "Punishment manager not available"));
        }

        if (plugin.getPunishmentManager().isMuted(data.uuid())) {
            return CompletableFuture.completedFuture(new PunishmentResult(false, "Player already muted"));
        }

        if (dryRun) {
            return CompletableFuture.completedFuture(
                    new PunishmentResult(true, "DRY RUN - Would mute: " + data.name())
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            plugin.getPunishmentManager().mutePlayer(
                    data.uuid(),
                    data.name(),
                    data.reason(),
                    data.muter(),
                    data.expires()
            );
            plugin.debug("Migrated mute for " + data.name());
            return new PunishmentResult(true, null);
        }, runnable -> plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run()));
    }

    public CompletableFuture<PunishmentResult> writeBan(BanTransfer data, boolean dryRun) {
        if (plugin.getPunishmentManager() == null) {
            return CompletableFuture.completedFuture(new PunishmentResult(false, "Punishment manager not available"));
        }

        if (plugin.getPunishmentManager().isBanned(data.uuid())) {
            return CompletableFuture.completedFuture(new PunishmentResult(false, "Player already banned"));
        }

        if (dryRun) {
            return CompletableFuture.completedFuture(
                    new PunishmentResult(true, "DRY RUN - Would ban: " + data.name())
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            plugin.getPunishmentManager().banPlayer(
                    data.uuid(),
                    data.name(),
                    data.reason(),
                    data.banner(),
                    data.expires()
            );
            plugin.debug("Migrated ban for " + data.name());
            return new PunishmentResult(true, null);
        }, runnable -> plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run()));
    }

    public record PunishmentResult(boolean success, String message) {}
}