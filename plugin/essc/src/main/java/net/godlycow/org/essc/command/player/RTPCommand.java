package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.rtp.RTPManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class RTPCommand extends Command {

    public RTPCommand(EssentialsC plugin) {
        super(plugin, "rtp", "essentialsc.rtp", true, 0, "command.usage.rtp");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!player.hasPermission("essentialsc.rtp")) {
            player.sendMessage(lang.get(player, "rtp.error.no_permission"));
            return true;
        }

        RTPManager manager = plugin.getRtpManager();

        if (!manager.isEnabled()) {
            player.sendMessage(lang.get(player, "rtp.error.disabled"));
            return true;
        }

        plugin.getRtpGuiManager().openGUI(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    public static void unregisterCommand() {
        try {
            CommandMap commandMap = Bukkit.getServer().getCommandMap();

            Field knownCommandsField = null;
            Class<?> clazz = commandMap.getClass();
            while (clazz != null && knownCommandsField == null) {
                try {
                    knownCommandsField = clazz.getDeclaredField("knownCommands");
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (knownCommandsField == null) {
                Bukkit.getLogger().warning("[EssentialsC] Could not locate knownCommands field to unregister /rtp");
                return;
            }

            knownCommandsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands =
                    (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            knownCommands.remove("rtp");
            knownCommands.remove("essentialsc:rtp");

            EssentialsC plugin = JavaPlugin.getPlugin(EssentialsC.class);
            plugin.getLogger().info("[EssentialsC] Successfully unregistered /rtp command.");

        } catch (IllegalAccessException e) {
            Bukkit.getLogger().warning("[EssentialsC] Failed to unregister /rtp command: " + e.getMessage());
        }
    }
}