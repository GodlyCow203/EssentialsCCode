package net.godlycow.org.essc.migration;

public record Options(
        boolean importUsers,
        boolean importWarps,
        boolean importEconomy,
        boolean importHomes,
        boolean importNicks,
        boolean importMutes,
        boolean importBans,
        boolean dryRun,
        ConflictStrategy conflictStrategy
) {
    public Options {
        if (conflictStrategy == null) conflictStrategy = ConflictStrategy.SKIP;
    }

    @Override
    public String toString() {
        return String.format(
                "users=%b, warps=%b, economy=%b, homes=%b, nicks=%b, mutes=%b, bans=%b, dryRun=%b, conflict=%s",
                importUsers, importWarps, importEconomy, importHomes,
                importNicks, importMutes, importBans, dryRun, conflictStrategy
        );
    }

    public enum ConflictStrategy {
        SKIP,
        OVERWRITE,
        RENAME,
        ABORT
    }
}