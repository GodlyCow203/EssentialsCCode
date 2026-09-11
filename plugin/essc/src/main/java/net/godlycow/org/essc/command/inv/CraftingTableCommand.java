package net.godlycow.org.essc.command.inv;


import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CraftingTableCommand extends Command {

    public CraftingTableCommand(EssentialsC plugin) {
        super(plugin, "craftingtable", "essentialsc.craftingtable", true, 0, "command.usage.craftingtable");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        player.openWorkbench(null, true);
        player.sendMessage(lang.get(player, "craftingtable.opened"));

        plugin.debug(player.getName() + " opened crafting table");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}