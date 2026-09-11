package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class RulesCommand extends Command {

    public RulesCommand(EssentialsC plugin) {
        super(plugin, "rules", "essentialsc.rules", false, 0, "command.usage.rules");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {

        List<Component> rules = plugin.getRulesManager().getRules();

        if (rules.isEmpty()) {
            sender.sendMessage(lang.get(sender, "rules.empty"));
            return true;
        }

        for (Component rule : rules) {
            sender.sendMessage(rule);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}