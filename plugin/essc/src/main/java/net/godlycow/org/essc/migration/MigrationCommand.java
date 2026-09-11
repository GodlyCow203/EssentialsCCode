package net.godlycow.org.essc.migration;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MigrationCommand extends Command {
    private EssMigrator currentMigrator;

    public MigrationCommand(EssentialsC plugin) {
        super(plugin, "migration", "essentialsc.migration", false, 0, "usage.migration");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(lang.get(sender, "migration.usage"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "essentialsx" -> handleEssentialsX(sender, args);
            case "status" -> handleStatus(sender);
            case "progress" -> handleProgress(sender);
            default -> sender.sendMessage(lang.get(sender, "migration.unknown_subcommand"));
        }

        return true;
    }

    private void handleEssentialsX(CommandSender sender, String[] args) {
        File essentialsFolder = new File(plugin.getDataFolder().getParentFile(), "Essentials");
        if (!essentialsFolder.exists()) {
            sender.sendMessage(lang.get(sender, "migration.essentialsx.not_found",
                    Map.of("path", essentialsFolder.getPath())));
            return;
        }

        Options options = parseOptions(args);

        if (options.dryRun()) {
            sender.sendMessage(lang.get(sender, "migration.essentialsx.dry_run"));
        }

        sender.sendMessage(lang.get(sender, "migration.essentialsx.starting"));
        sender.sendMessage(lang.get(sender, "migration.essentialsx.importing", Map.of(
                "users", String.valueOf(options.importUsers()),
                "warps", String.valueOf(options.importWarps()),
                "economy", String.valueOf(options.importEconomy()),
                "homes", String.valueOf(options.importHomes()),
                "nicks", String.valueOf(options.importNicks()),
                "mutes", String.valueOf(options.importMutes()),
                "bans", String.valueOf(options.importBans()),
                "dryrun", String.valueOf(options.dryRun()),
                "conflict", options.conflictStrategy().toString()
        )));

        plugin.debug("Starting EssentialsX migration with options: " + options);

        currentMigrator = new EssMigrator(plugin, essentialsFolder);

        if (!options.dryRun()) {
            startProgressTask(sender);
        }

        currentMigrator.migrate(options)
                .thenAccept(result -> {
                    plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                        sender.sendMessage(lang.get(sender, "migration.essentialsx.complete"));
                        sender.sendMessage(lang.get(sender, "migration.stats", Map.ofEntries(
                                Map.entry("users", String.valueOf(result.usersMigrated())),
                                Map.entry("usersFailed", String.valueOf(result.usersFailed())),
                                Map.entry("warps", String.valueOf(result.warpsMigrated())),
                                Map.entry("warpsFailed", String.valueOf(result.warpsFailed())),
                                Map.entry("homes", String.valueOf(result.homesMigrated())),
                                Map.entry("homesSkipped", String.valueOf(result.homesSkipped())),
                                Map.entry("economy", String.valueOf(result.economyRecords())),
                                Map.entry("nicks", String.valueOf(result.nicknamesMigrated())),
                                Map.entry("mutes", String.valueOf(result.mutesMigrated())),
                                Map.entry("bans", String.valueOf(result.bansMigrated())),
                                Map.entry("bansFailed", String.valueOf(result.bansFailed())),
                                Map.entry("warnings", String.valueOf(result.warnings().size()))
                        )));


                        if (!result.warnings().isEmpty()) {
                            sender.sendMessage(lang.get(sender, "migration.warnings_header"));
                            result.warnings().stream().limit(5).forEach(w ->
                                    sender.sendMessage(lang.get(sender, "migration.warning_entry", Map.of("msg", w)))
                            );
                            if (result.warnings().size() > 5) {
                                sender.sendMessage(lang.get(sender, "migration.warnings_more",
                                        Map.of("count", String.valueOf(result.warnings().size() - 5))));
                            }
                        }

                        plugin.debug("Migration completed: " + result);
                        currentMigrator = null;
                    });
                })
                .exceptionally(ex -> {
                    plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        sender.sendMessage(lang.get(sender, "migration.error", Map.of("error", msg)));
                        plugin.debug("Migration failed: " + msg);
                        ex.printStackTrace();
                        currentMigrator = null;
                    });
                    return null;
                });
    }

    private void startProgressTask(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (currentMigrator == null) {
                task.cancel();
                return;
            }

            MigrationProgress MigrationProgress = currentMigrator.getProgress();
            if (MigrationProgress.getPercentComplete() < 100) {
                sender.sendMessage(lang.get(sender, "migration.progress", Map.of(
                        "stage", MigrationProgress.getCurrentStage(),
                        "percent", String.valueOf(MigrationProgress.getPercentComplete()),
                        "eta", MigrationProgress.getEstimatedTimeRemaining()
                )));
            }
        }, 100L, 200L);
    }

    private void handleProgress(CommandSender sender) {
        if (currentMigrator == null) {
            sender.sendMessage(lang.get(sender, "migration.no_active"));
            return;
        }

        MigrationProgress MigrationProgress = currentMigrator.getProgress();
        sender.sendMessage(lang.get(sender, "migration.progress_detail", Map.of(
                "stage", MigrationProgress.getCurrentStage(),
                "users", MigrationProgress.getProcessedUsers() + "/" + MigrationProgress.getTotalUsers(),
                "warps", MigrationProgress.getProcessedWarps() + "/" + MigrationProgress.getTotalWarps(),
                "percent", String.valueOf(MigrationProgress.getPercentComplete()),
                "elapsed", String.valueOf(MigrationProgress.getElapsedTimeMs() / 1000) + "s",
                "eta", MigrationProgress.getEstimatedTimeRemaining()
        )));
    }

    private void handleStatus(CommandSender sender) {
        File essentialsFolder = new File(plugin.getDataFolder().getParentFile(), "Essentials");
        File bannedPlayersFile = new File(plugin.getDataFolder().getParentFile().getParentFile(), "banned-players.json");

        sender.sendMessage(lang.get(sender, "migration.status.header"));

        boolean essentialsFound = essentialsFolder.exists();
        sender.sendMessage(lang.get(sender, "migration.status.essentialsx",
                Map.of("status", essentialsFound ? "<green>Found" : "<red>Not found")));

        if (essentialsFound) {
            File userdata = new File(essentialsFolder, "userdata");
            File warps = new File(essentialsFolder, "warps");

            int userCount = userdata.exists() ? userdata.listFiles((d, n) -> n.endsWith(".yml")).length : 0;
            int warpCount = warps.exists() ? warps.listFiles((d, n) -> n.endsWith(".yml")).length : 0;

            sender.sendMessage(lang.get(sender, "migration.status.users",
                    Map.of("count", String.valueOf(userCount))));
            sender.sendMessage(lang.get(sender, "migration.status.warps",
                    Map.of("count", String.valueOf(warpCount))));
        }

        boolean bansFound = bannedPlayersFile.exists();
        sender.sendMessage(lang.get(sender, "migration.status.bans",
                Map.of("status", bansFound ? "<green>Found" : "<red>Not found")));

        sender.sendMessage(lang.get(sender, "migration.status.managers", Map.of(
                "economy", plugin.getEconomyManager() != null ? "<green>OK" : "<red>Missing",
                "homes", plugin.getHomeManager() != null ? "<green>OK" : "<red>Missing",
                "nicks", plugin.getNickManager() != null ? "<green>OK" : "<red>Missing",
                "warps", plugin.getWarpManager() != null ? "<green>OK" : "<red>Missing",
                "punishments", plugin.getPunishmentManager() != null ? "<green>OK" : "<red>Missing"
        )));
    }

    private Options parseOptions(String[] args) {
        boolean importUsers = true;
        boolean importWarps = true;
        boolean importEconomy = true;
        boolean importHomes = true;
        boolean importNicks = true;
        boolean importMutes = true;
        boolean importBans = true;
        boolean dryRun = false;
        Options.ConflictStrategy conflictStrategy = Options.ConflictStrategy.SKIP;

        for (int i = 1; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "--no-economy" -> importEconomy = false;
                case "--no-homes" -> importHomes = false;
                case "--no-warps" -> importWarps = false;
                case "--no-nicks" -> importNicks = false;
                case "--no-mutes" -> importMutes = false;
                case "--no-bans" -> importBans = false;
                case "--dry-run" -> dryRun = true;
                case "--overwrite" -> conflictStrategy = Options.ConflictStrategy.OVERWRITE;
                case "--rename-conflicts" -> conflictStrategy = Options.ConflictStrategy.RENAME;
                case "--abort-on-conflict" -> conflictStrategy = Options.ConflictStrategy.ABORT;
                case "--only-warps" -> {
                    importUsers = false;
                    importWarps = true;
                    importBans = false;
                }
                case "--only-bans" -> {
                    importUsers = false;
                    importWarps = false;
                    importBans = true;
                }
                case "--only-mutes" -> {
                    importUsers = true;
                    importWarps = false;
                    importEconomy = false;
                    importHomes = false;
                    importNicks = false;
                    importBans = false;
                }
            }
        }

        return new Options(importUsers, importWarps, importEconomy,
                importHomes, importNicks, importMutes, importBans, dryRun, conflictStrategy);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("essentialsx", "status", "progress");
        } else if (args.length > 1 && args[0].equalsIgnoreCase("essentialsx")) {
            return List.of("--no-economy", "--no-homes", "--no-warps",
                    "--no-nicks", "--no-mutes", "--no-bans",
                    "--dry-run", "--overwrite", "--rename-conflicts", "--abort-on-conflict",
                    "--only-warps", "--only-bans", "--only-mutes");
        }
        return List.of();
    }
}