package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepairCommand extends Command {

    public RepairCommand(EssentialsC plugin) {
        super(plugin, "repair", "essentialsc.repair", true, 0, "command.usage.repair");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Player target;

        if (args.length > 0) {
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[0]);
                player.sendMessage(lang.get(player, "error.player_not_found", placeholders));
                return true;
            }

            if (target != player && !player.hasPermission("essentialsc.repair.others")) {
                player.sendMessage(lang.get(player, "error.no_permission"));
                plugin.debug("Denied: " + player.getName() + " lacks permission essentialsc.repair.others");
                return true;
            }
        } else {
            target = player;
        }

        ItemStack item = target.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            player.sendMessage(lang.get(player, "repair.error.no_item"));
            plugin.debug("Repair failed: " + target.getName() + " is not holding an item");
            return true;
        }

        if (!(item.getItemMeta() instanceof Damageable damageable)) {
            player.sendMessage(lang.get(player, "repair.error.not_repairable"));
            plugin.debug("Repair failed: Item " + item.getType() + " is not damageable");
            return true;
        }

        if (damageable.getDamage() == 0) {
            player.sendMessage(lang.get(player, "repair.error.already_repaired"));
            plugin.debug("Repair failed: Item is already at full durability");
            return true;
        }

        plugin.debug("Repairing item " + item.getType() + " for " + target.getName() + " by " + player.getName());

        damageable.setDamage(0);
        item.setItemMeta((ItemMeta) damageable);

        String itemName = item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().toString().toLowerCase().replace("_", " ");

        plugin.debug("Successfully repaired " + itemName + " for " + target.getName());

        if (target == player) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("item", itemName);
            player.sendMessage(lang.get(player, "repair.success", placeholders));
        } else {
            Map<String, String> senderPlaceholders = new HashMap<>();
            senderPlaceholders.put("target", target.getName());
            senderPlaceholders.put("item", itemName);
            player.sendMessage(lang.get(player, "repair.success.other", senderPlaceholders));

            Map<String, String> targetPlaceholders = new HashMap<>();
            targetPlaceholders.put("repairer", player.getName());
            targetPlaceholders.put("item", itemName);
            target.sendMessage(lang.get(target, "repair.success.by", targetPlaceholders));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("essentialsc.repair.others")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}