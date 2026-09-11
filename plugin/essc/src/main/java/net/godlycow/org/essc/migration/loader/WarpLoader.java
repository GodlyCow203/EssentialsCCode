package net.godlycow.org.essc.migration.loader;

import net.godlycow.org.essc.migration.model.EssLocation;
import net.godlycow.org.essc.migration.model.EssWarp;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WarpLoader {
    private static final Logger LOGGER = Logger.getLogger(WarpLoader.class.getName());

    public EssWarp read(File file) {
        if (!file.exists()) return null;

        try {
            String warpName = file.getName().replace(".yml", "");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            String worldStr = config.getString("world");
            String worldName = config.getString("world-name", "world");

            if (worldStr == null) {
                LOGGER.log(Level.WARNING, "No world specified for warp: " + warpName);
                return null;
            }

            UUID worldUuid = null;
            try {
                worldUuid = UUID.fromString(worldStr);
            } catch (IllegalArgumentException e) {
                worldName = worldStr;
            }

            EssLocation location = new EssLocation(
                    worldUuid,
                    worldName,
                    config.getDouble("x"),
                    config.getDouble("y"),
                    config.getDouble("z"),
                    (float) config.getDouble("yaw", 0.0),
                    (float) config.getDouble("pitch", 0.0)
            );

            String lastOwnerStr = config.getString("lastowner");
            UUID lastOwner = null;
            if (lastOwnerStr != null) {
                try {
                    lastOwner = UUID.fromString(lastOwnerStr);
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING, "Invalid lastowner UUID for warp: " + warpName);
                }
            }

            return new EssWarp(warpName, location, lastOwner);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to read warp file: " + file.getName(), e);
            return null;
        }
    }
}