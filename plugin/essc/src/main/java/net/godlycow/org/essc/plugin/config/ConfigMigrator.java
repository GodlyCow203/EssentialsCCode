package net.godlycow.org.essc.plugin.config;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigMigrator {

    private static final int CURRENT_CONFIG_VERSION = 15;
    private static final int CURRENT_COMMANDS_VERSION = 8;

    private final EssentialsC plugin;

    public ConfigMigrator(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public void migrateConfig() {
        FileConfiguration config = plugin.getConfig();
        int version = config.getInt("config-version", 0);

        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream == null) {
            return;
        }

        FileConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
        );

        boolean dirty = false;

        for (String key : defaults.getKeys(true)) {
            if (key.equals("config-version")) {
                continue;
            }
            if (key.startsWith("rtp.worlds.")) {
                continue;
            }
            if (!defaults.isConfigurationSection(key) && !config.isSet(key)) {
                config.set(key, defaults.get(key));
                plugin.debug("[EssentialsC] config.yml — added missing key: " + key);
                dirty = true;
            }
        }

        if (version != CURRENT_CONFIG_VERSION) {
            runConfigVersionMigrations(config, version);
            config.set("config-version", CURRENT_CONFIG_VERSION);
            dirty = true;
            plugin.debug("[EssentialsC] config.yml updated from version " + version + " to " + CURRENT_CONFIG_VERSION);
        }

        if (dirty) {
            plugin.saveConfig();
        }
    }

    public void migrateCommands(FileConfiguration config, File file) {
        int version = config.getInt("config-version", 0);

        InputStream defaultStream = plugin.getResource("commands.yml");
        if (defaultStream == null) {
            return;
        }

        FileConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
        );

        boolean dirty = false;

        for (String key : defaults.getKeys(true)) {
            if (key.equals("config-version")) {
                continue;
            }
            if (!config.isSet(key)) {
                config.set(key, defaults.get(key));
                plugin.debug("[EssentialsC] commands.yml — added missing key: " + key);
                dirty = true;
            }
        }

        if (version != CURRENT_COMMANDS_VERSION) {
            runCommandsVersionMigrations(config, version);
            config.set("config-version", CURRENT_COMMANDS_VERSION);
            dirty = true;
            plugin.debug("[EssentialsC] commands.yml updated from version " + version + " to " + CURRENT_COMMANDS_VERSION);
        }

        if (dirty) {
            saveYaml(config, file, "commands.yml");
        }
    }

    private void runConfigVersionMigrations(FileConfiguration config, int fromVersion) {
        if (fromVersion < 1) {
            applyConfigV1Migrations(config);
        }
    }

    private void applyConfigV1Migrations(FileConfiguration config) {
    }

    private void runCommandsVersionMigrations(FileConfiguration config, int fromVersion) {
        if (fromVersion < 1) {
            applyCommandsV1Migrations(config);
        }
    }

    private void applyCommandsV1Migrations(FileConfiguration config) {
    }

    private void saveYaml(FileConfiguration config, File file, String name) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[EssentialsC] Could not save " + name + ": " + e.getMessage());
        }
    }
}