package net.godlycow.org.essc.migration.saver;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.migration.resolver.ConflictResolver;
import net.godlycow.org.essc.migration.mapper.UserDataMapper.NicknameTransfer;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class Nick {
    private final EssentialsC plugin;
    private final ConflictResolver conflictResolver;

    public Nick(EssentialsC plugin, ConflictResolver conflictResolver) {
        this.plugin = plugin;
        this.conflictResolver = conflictResolver;
    }

    public CompletableFuture<NicknameResult> write(NicknameTransfer data, boolean dryRun) {
        if (plugin.getNickManager() == null) {
            return CompletableFuture.completedFuture(new NicknameResult(false, "Nick manager not available", null));
        }

        return conflictResolver.resolveNicknameConflict(data.uuid()).thenCompose(resolution -> {
            return switch (resolution.action()) {
                case SKIP -> CompletableFuture.completedFuture(
                        new NicknameResult(false, "Skipped - nickname already exists", null)
                );
                case ABORT -> CompletableFuture.failedFuture(
                        new RuntimeException("Conflict abort requested for nickname: " + data.uuid())
                );
                case OVERWRITE, CREATE -> applyNickname(data, dryRun);
            };
        });
    }

    private CompletableFuture<NicknameResult> applyNickname(NicknameTransfer data, boolean dryRun) {
        if (dryRun) {
            return CompletableFuture.completedFuture(
                    new NicknameResult(true, "DRY RUN - Would set nickname: " + data.nickname(), null)
            );
        }

        return plugin.getNickManager().setNickname(data.uuid(), data.nickname())
                .thenApply(success -> {
                    if (!success) {
                        return new NicknameResult(false, "setNickname returned false", null);
                    }

                    Player onlinePlayer = plugin.getServer().getPlayer(data.uuid());
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        onlinePlayer.getScheduler().run(plugin, task -> {
                            plugin.getNickManager().applyNickname(onlinePlayer);
                            plugin.debug("Applied migrated nickname to online player: " + onlinePlayer.getName());
                        }, null);
                    } else {
                        plugin.debug("Nickname saved for offline player " + data.uuid() + " - will apply on next join");
                    }

                    plugin.debug("Migrated nickname for " + data.uuid() + ": " + data.nickname());
                    return new NicknameResult(true, null, data.nickname());
                });
    }

    public record NicknameResult(boolean success, String error, String appliedNick) {}
}