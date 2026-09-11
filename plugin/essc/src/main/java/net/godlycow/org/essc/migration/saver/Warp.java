package net.godlycow.org.essc.migration.saver;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.migration.resolver.ConflictResolver;
import net.godlycow.org.essc.migration.mapper.WarpMapper.WarpTransfer;

import java.util.concurrent.CompletableFuture;

public class Warp {
    private final EssentialsC plugin;
    private final ConflictResolver conflictResolver;

    public Warp(EssentialsC plugin, ConflictResolver conflictResolver) {
        this.plugin = plugin;
        this.conflictResolver = conflictResolver;
    }

    public CompletableFuture<WarpResult> write(WarpTransfer data, boolean dryRun) {
        if (plugin.getWarpManager() == null) {
            return CompletableFuture.completedFuture(new WarpResult(false, "Warp manager not available", null));
        }

        return conflictResolver.resolveWarpConflict(data.name()).thenCompose(resolution -> {
            String targetName = resolution.newName() != null ? resolution.newName() : data.name();

            return switch (resolution.action()) {
                case SKIP -> CompletableFuture.completedFuture(
                        new WarpResult(false, "Skipped - warp already exists", null)
                );
                case ABORT -> CompletableFuture.failedFuture(
                        new RuntimeException("Conflict abort requested for warp: " + data.name())
                );
                case OVERWRITE -> plugin.getWarpManager()
                        .deleteWarp(data.name())
                        .thenCompose(deleted -> createWarp(targetName, data, dryRun));

                case CREATE -> createWarp(targetName, data, dryRun);
            };
        });
    }

    private CompletableFuture<WarpResult> createWarp(String name, WarpTransfer data, boolean dryRun) {
        if (dryRun) {
            return CompletableFuture.completedFuture(
                    new WarpResult(true, "DRY RUN - Would create warp: " + name, name)
            );
        }

        return plugin.getWarpManager().createWarp(name, data.location()).thenCompose(created -> {
            if (!created) {
                return CompletableFuture.completedFuture(
                        new WarpResult(false, "createWarp returned false", null)
                );
            }

            net.godlycow.org.essc.modules.warp.Warp warp = plugin.getWarpManager().getWarp(name);
            if (warp == null) {
                return CompletableFuture.completedFuture(
                        new WarpResult(false, "Failed to retrieve created warp", null)
                );
            }

            warp.setPermission(data.permission());
            warp.setCost(data.cost());
            warp.setHidden(data.hidden());
            warp.setDescription(data.description());
            warp.setCategory(data.category());

            return plugin.getWarpManager().updateWarp(warp).thenApply(updated -> {
                if (updated) {
                    plugin.debug("Migrated warp: " + name + " with properties");
                }

                return new WarpResult(
                        updated,
                        updated ? null : "updateWarp failed",
                        name
                );
            });
        });
    }

    public record WarpResult(boolean success, String error, String finalName) {}
}