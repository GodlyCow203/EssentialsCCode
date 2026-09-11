package net.godlycow.org.essc.plugin.config;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class CommandsConfig {

    private final EssentialsC plugin;
    private final File file;
    private FileConfiguration config;
    private final ConfigMigrator migrator;

    public CommandsConfig(EssentialsC plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "commands.yml");
        this.migrator = new ConfigMigrator(plugin);
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("commands.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        migrate();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        migrate();
        plugin.debug("commands.yml reloaded");
    }

    public void migrate() {
        migrator.migrateCommands(config, file);
    }

    public boolean isEnabled(String command) {
        return config.getBoolean(command + ".enabled", true);
    }

    public String getPriority(String command) {
        return config.getString(command + ".priority", "normal").toLowerCase();
    }

    public List<String> getAliases(String command) {
        return config.getStringList(command + ".aliases");
    }

    public long getCooldown(String command) {
        return config.getLong(command + ".cooldown", 0L);
    }

    public String getCooldownBypassPermission(String command) {
        return config.getString(command + ".cooldown-bypass", null);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}