package net.godlycow.org.essc.migration.resolver;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.migration.Options;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MigrationValidator {
    private final EssentialsC plugin;
    private final File essentialsDataFolder;

    public MigrationValidator(EssentialsC plugin, File essentialsDataFolder) {
        this.plugin = plugin;
        this.essentialsDataFolder = essentialsDataFolder;
    }

    public CompletableFuture<ValidationReport> validate(Options options) {
        return CompletableFuture.supplyAsync(() -> {
            ValidationReport report = new ValidationReport();

            if (!essentialsDataFolder.exists()) {
                report.addError("EssentialsX folder not found: " + essentialsDataFolder.getPath());
                return report;
            }

            if (options.importUsers() || options.importWarps()) {
                validateWorlds(report);
            }

            if (options.importUsers()) {
                File usersDir = new File(essentialsDataFolder, "userdata");
                if (usersDir.exists()) {
                    File[] files = usersDir.listFiles((d, n) -> n.endsWith(".yml"));
                    report.setUserCount(files != null ? files.length : 0);
                }
            }

            if (options.importWarps()) {
                File warpsDir = new File(essentialsDataFolder, "warps");
                if (warpsDir.exists()) {
                    File[] files = warpsDir.listFiles((d, n) -> n.endsWith(".yml"));
                    report.setWarpCount(files != null ? files.length : 0);
                }
            }

            validateManagers(options, report);

            return report;
        });
    }

    private void validateWorlds(ValidationReport report) {
        File usersDir = new File(essentialsDataFolder, "userdata");
        if (!usersDir.exists()) return;

        File[] files = usersDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        Set<String> missingWorlds = new HashSet<>();
        Set<String> foundWorlds = new HashSet<>();

        for (File file : files) {
            try (java.util.Scanner scanner = new java.util.Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.trim().startsWith("world:")) {
                        String worldId = line.substring(line.indexOf(":") + 1).trim();
                        if (!worldId.isEmpty()) {
                            try {
                                UUID worldUuid = UUID.fromString(worldId);
                                World world = Bukkit.getWorld(worldUuid);
                                if (world == null) {
                                    missingWorlds.add(worldId.substring(0, Math.min(8, worldId.length())) + "...");
                                } else {
                                    foundWorlds.add(world.getName());
                                }
                            } catch (IllegalArgumentException e) {
                                World world = Bukkit.getWorld(worldId);
                                if (world == null) {
                                    missingWorlds.add(worldId);
                                } else {
                                    foundWorlds.add(worldId);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        report.setMissingWorlds(missingWorlds);
        report.setFoundWorlds(foundWorlds);
    }

    private void validateManagers(Options options, ValidationReport report) {
        if (options.importEconomy() && plugin.getEconomyManager() == null) {
            report.addWarning("Economy manager not available - economy import will be skipped");
        }
        if (options.importHomes() && plugin.getHomeManager() == null) {
            report.addWarning("Home manager not available - homes import will be skipped");
        }
        if (options.importNicks() && plugin.getNickManager() == null) {
            report.addWarning("Nick manager not available - nicknames import will be skipped");
        }
        if (options.importWarps() && plugin.getWarpManager() == null) {
            report.addWarning("Warp manager not available - warps import will be skipped");
        }
        if (options.importMutes() && plugin.getPunishmentManager() == null) {
            report.addWarning("Punishment manager not available - mutes import will be skipped");
        }
    }

    public static class ValidationReport {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private int userCount = 0;
        private int warpCount = 0;
        private Set<String> missingWorlds = new HashSet<>();
        private Set<String> foundWorlds = new HashSet<>();

        public void addError(String error) {
            errors.add(error);
        }
        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }
        public List<String> getErrors() {
            return errors;
        }
        public List<String> getWarnings() {
            return warnings;
        }

        public void setUserCount(int count) {
            this.userCount = count;
        }
        public void setWarpCount(int count) {
            this.warpCount = count;
        }
        public void setMissingWorlds(Set<String> worlds) {
            this.missingWorlds = worlds;
        }
        public void setFoundWorlds(Set<String> worlds) {
            this.foundWorlds = worlds;
        }

        public int getUserCount() {
            return userCount;
        }
        public int getWarpCount() {
            return warpCount;
        }

        @Override
        public String toString() {
            return String.format("ValidationReport[valid=%b, users=%d, warps=%d, errors=%d, warnings=%d, missingWorlds=%s]",
                    isValid(), userCount, warpCount, errors.size(), warnings.size(), missingWorlds);
        }
    }
}