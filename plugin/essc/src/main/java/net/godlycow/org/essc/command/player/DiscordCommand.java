package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class DiscordCommand extends Command {

    public DiscordCommand(EssentialsC plugin) {
        super(plugin, "discord", "essentialsc.discord", false, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        List<String> lines = plugin.getConfigManager().getDiscordMessage();
        MiniMessage mm = plugin.getMiniMessage();

        for (String line : lines) {
            sender.sendMessage(mm.deserialize(line));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}