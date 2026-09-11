package net.godlycow.org.essc.migration.resolver;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.migration.Options.ConflictStrategy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConflictResolver {
    private final EssentialsC plugin;
    private final ConflictStrategy strategy;

    public ConflictResolver(EssentialsC plugin, ConflictStrategy strategy) {
        this.plugin = plugin;
        this.strategy = strategy;
    }

    public CompletableFuture<Resolution> resolveWarpConflict(String warpName) {
        return CompletableFuture.supplyAsync(() -> {
            if (plugin.getWarpManager() == null || !plugin.getWarpManager().warpExists(warpName)) {
                return new Resolution(ResolutionAction.CREATE, warpName);
            }

            return switch (strategy) {
                case SKIP -> new Resolution(ResolutionAction.SKIP, warpName);
                case OVERWRITE -> new Resolution(ResolutionAction.OVERWRITE, warpName);
                case RENAME -> new Resolution(ResolutionAction.CREATE, warpName + "_migrated");
                case ABORT -> new Resolution(ResolutionAction.ABORT, warpName);
            };
        });
    }

    public CompletableFuture<Resolution> resolveHomeConflict(UUID owner, String homeName) {
        return plugin.getHomeManager().homeExists(owner, homeName).thenApply(exists -> {
            if (!exists) return new Resolution(ResolutionAction.CREATE, homeName);

            return switch (strategy) {
                case SKIP -> new Resolution(ResolutionAction.SKIP, homeName);
                case OVERWRITE -> new Resolution(ResolutionAction.OVERWRITE, homeName);
                case RENAME -> new Resolution(ResolutionAction.CREATE, homeName + "_backup");
                case ABORT -> new Resolution(ResolutionAction.ABORT, homeName);
            };
        });
    }

    public CompletableFuture<Resolution> resolveNicknameConflict(UUID uuid) {
        return plugin.getNickManager().getNickname(uuid).thenApply(opt -> {
            if (opt.isEmpty()) return new Resolution(ResolutionAction.CREATE, null);

            return switch (strategy) {
                case SKIP, RENAME -> new Resolution(ResolutionAction.SKIP, null);
                case OVERWRITE -> new Resolution(ResolutionAction.OVERWRITE, null);
                case ABORT -> new Resolution(ResolutionAction.ABORT, null);
            };
        });
    }

    public record Resolution(ResolutionAction action, String newName) {}

    public enum ResolutionAction {
        CREATE, SKIP, OVERWRITE, ABORT
    }
}