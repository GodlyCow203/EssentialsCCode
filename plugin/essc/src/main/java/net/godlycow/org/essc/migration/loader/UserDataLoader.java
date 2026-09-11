package net.godlycow.org.essc.migration.loader;

import net.godlycow.org.essc.migration.model.EssHome;
import net.godlycow.org.essc.migration.model.EssLocation;
import net.godlycow.org.essc.migration.model.EssUserData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDataLoader {
    private static final Logger LOGGER = Logger.getLogger(UserDataLoader.class.getName());

    public EssUserData read(File file) {
        if (!file.exists()) return null;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String filename = file.getName().replace(".yml", "");
            UUID uuid = UUID.fromString(filename);

            return new EssUserData(
                    uuid,
                    config.getString("last-account-name", ""),
                    parseMoney(config.getString("money", "0")),
                    config.getBoolean("accepting-pay", true),
                    config.getBoolean("baltop-exempt", false),
                    config.getBoolean("teleportenabled", true),
                    config.getBoolean("teleportauto", false),
                    readLocation(config, "lastlocation"),
                    readLocation(config, "logoutlocation"),
                    readHomes(config.getConfigurationSection("homes")),
                    config.getLong("timestamps.lastteleport", 0),
                    config.getBoolean("godmode", false),
                    config.getBoolean("muted", false),
                    config.getString("mute-reason", null),
                    config.getBoolean("jailed", false),
                    config.getBoolean("afk", false),
                    config.getBoolean("socialspy", false),
                    config.getBoolean("npc", false),
                    config.getLong("timestamps.lastheal", 0),
                    config.getLong("timestamps.mute", 0),
                    config.getLong("timestamps.jail", 0),
                    config.getLong("timestamps.onlinejail", 0),
                    config.getLong("timestamps.logout", 0),
                    config.getLong("timestamps.login", 0),
                    config.getString("ip-address", "127.0.0.1"),
                    config.getBoolean("powertoolsenabled", true),
                    config.getString("nickname", null)
            );
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid UUID in filename: " + file.getName(), e);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to read user file: " + file.getName(), e);
            return null;
        }
    }

    private BigDecimal parseMoney(String moneyStr) {
        if (moneyStr == null || moneyStr.isEmpty()) return BigDecimal.ZERO;

        try {
            return new BigDecimal(moneyStr);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid money format: " + moneyStr);
            return BigDecimal.ZERO;
        }
    }

    private EssLocation readLocation(YamlConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return null;

        String worldStr = section.getString("world");
        String worldName = section.getString("world-name", "world");

        if (worldStr == null) {
            if (worldName != null && !worldName.isEmpty()) {
                worldStr = worldName;
            } else {
                return null;
            }
        }

        try {
            UUID worldUuid;
            try {
                worldUuid = UUID.fromString(worldStr);
            } catch (IllegalArgumentException e) {
                worldUuid = null;
                worldName = worldStr;
            }

            return new EssLocation(
                    worldUuid,
                    worldName,
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw", 0.0),
                    (float) section.getDouble("pitch", 0.0)
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read location at " + path, e);
            return null;
        }
    }

    private Map<String, EssHome> readHomes(ConfigurationSection homesSection) {
        Map<String, EssHome> homes = new HashMap<>();
        if (homesSection == null) return homes;

        for (String homeName : homesSection.getKeys(false)) {
            try {
                ConfigurationSection homeSection = homesSection.getConfigurationSection(homeName);
                if (homeSection == null) continue;

                String worldStr = homeSection.getString("world");
                String worldName = homeSection.getString("world-name", "world");

                if (worldStr == null) continue;

                UUID worldUuid = null;
                try {
                    worldUuid = UUID.fromString(worldStr);
                } catch (IllegalArgumentException e) {
                    worldName = worldStr;
                }

                EssLocation loc = new EssLocation(
                        worldUuid,
                        worldName,
                        homeSection.getDouble("x"),
                        homeSection.getDouble("y"),
                        homeSection.getDouble("z"),
                        (float) homeSection.getDouble("yaw", 0.0),
                        (float) homeSection.getDouble("pitch", 0.0)
                );

                homes.put(homeName.toLowerCase(), new EssHome(homeName, loc));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to read home: " + homeName, e);
            }
        }

        return homes;
    }
}