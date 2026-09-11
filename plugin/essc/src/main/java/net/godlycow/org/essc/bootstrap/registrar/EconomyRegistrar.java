package net.godlycow.org.essc.bootstrap.registrar;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.economy.*;
import net.godlycow.org.essc.plugin.economy.EconomyManager;
import net.godlycow.org.essc.plugin.economy.VaultHook;
import net.godlycow.org.essc.CommandRegistration;
import org.bukkit.command.PluginCommand;

import java.util.List;

public class EconomyRegistrar {

    private static final List<String> ECONOMY_COMMANDS =
            List.of("balance", "pay", "eco", "baltop");

    private final EssentialsC plugin;

    public EconomyRegistrar(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (plugin.getEconomyManager() == null) {
            EconomyManager economyManager = new EconomyManager(plugin);
            plugin.setEconomyManager(economyManager);

            VaultHook vaultHook = new VaultHook(economyManager);
            plugin.setVaultHook(vaultHook);

            if (vaultHook.hook()) {
                plugin.debug("Successfully hooked into Vault!");
            } else {
                plugin.debug("Using built-in economy system");
            }
        }

        register("balance", new BalanceCommand(plugin));
        register("pay", new PayCommand(plugin));
        register("eco", new EcoCommand(plugin));
        register("baltop", new BaltopCommand(plugin));

        CommandRegistration.syncCommands();
    }

    public void disable() {
        if (plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().shutdown();
            plugin.setEconomyManager(null);
            plugin.setVaultHook(null);
        }

        unregisterCommands();
    }

    private void unregisterCommands() {
        for (String cmd : ECONOMY_COMMANDS) {
            PluginCommand command = plugin.getCommand(cmd);
            if (command != null) {
                command.setExecutor(null);
                command.setTabCompleter(null);
            }
        }

        CommandRegistration.unregisterCommands(ECONOMY_COMMANDS);
        CommandRegistration.syncCommands();
    }

    private void register(String name, net.godlycow.org.essc.command.Command command) {
        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand == null) {
            plugin.getLogger().warning("Command '" + name + "' not found in plugin.yml");
            return;
        }

        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        List<String> configAliases = plugin.getCommandsConfig().getAliases(name);
        for (String alias : configAliases) {
            CommandRegistration.registerAlias(alias, pluginCommand);
            plugin.debug("Registered alias '/" + alias + "' -> '" + name + "'");
        }
    }
}
