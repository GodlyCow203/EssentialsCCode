package net.godlycow.org.essc.command.item;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemIdCommand extends Command {

    public ItemIdCommand(EssentialsC plugin) {
        super(plugin, "itemid", "essentialsc.itemid", true, 0, "command.usage.itemid");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            player.sendMessage(lang.get(player, "itemid.empty_hand"));
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("material", item.getType().name());
        placeholders.put("id", String.valueOf(item.getType().getKey().getKey()));
        placeholders.put("max_stack", String.valueOf(item.getMaxStackSize()));
        placeholders.put("amount", String.valueOf(item.getAmount()));
        placeholders.put("durability", String.valueOf(item.getDurability()));
        placeholders.put("max_durability", String.valueOf(item.getType().getMaxDurability()));

        player.sendMessage(lang.get(player, "itemid.header", placeholders));

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                Map<String, String> namePlaceholders = new HashMap<>();
                namePlaceholders.put("name", displayName);
                player.sendMessage(lang.get(player, "itemid.display_name", namePlaceholders));
            }

            if (meta.hasLore() && meta.getLore() != null) {
                String lore = String.join(", ", meta.getLore());
                Map<String, String> lorePlaceholders = new HashMap<>();
                lorePlaceholders.put("lore", lore);
                player.sendMessage(lang.get(player, "itemid.lore", lorePlaceholders));
            }

            if (meta.hasCustomModelData()) {
                Map<String, String> cmdPlaceholders = new HashMap<>();
                cmdPlaceholders.put("cmd", String.valueOf(meta.getCustomModelData()));
                player.sendMessage(lang.get(player, "itemid.custom_model_data", cmdPlaceholders));
            }
        }

        if (!item.getEnchantments().isEmpty()) {
            StringBuilder enchants = new StringBuilder();
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                if (!enchants.isEmpty()) enchants.append(", ");
                enchants.append(entry.getKey().getKey().getKey())
                        .append(" (")
                        .append(entry.getValue())
                        .append(")");
            }
            Map<String, String> enchantPlaceholders = new HashMap<>();
            enchantPlaceholders.put("enchantments", enchants.toString());
            player.sendMessage(lang.get(player, "itemid.enchantments", enchantPlaceholders));
        }

        if (item.getType().getMaxDurability() > 0) {
            int remaining = item.getType().getMaxDurability() - item.getDurability();
            double percent = ((double) remaining / item.getType().getMaxDurability()) * 100;
            Map<String, String> durabilityPlaceholders = new HashMap<>();
            durabilityPlaceholders.put("remaining", String.valueOf(remaining));
            durabilityPlaceholders.put("percent", String.format("%.1f", percent));
            player.sendMessage(lang.get(player, "itemid.durability_info", durabilityPlaceholders));
        }

        player.sendMessage(lang.get(player, "itemid.footer"));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}