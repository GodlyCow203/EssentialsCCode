package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealCommand extends Command {

    public HealCommand(EssentialsC plugin) {
        super(plugin, "heal", "essentialsc.heal", true, 0, "command.usage.heal");
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

            if (target != player && !player.hasPermission("essentialsc.heal.others")) {
                player.sendMessage(lang.get(player, "error.no_permission"));
                plugin.debug("Denied: " + player.getName() + " lacks permission essentialsc.heal.others");
                return true;
            }
        } else {
            target = player;
        }

        plugin.debug("Healing initiated for " + target.getName() + " by " + player.getName());

        double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        target.setHealth(maxHealth);
        plugin.debug("Set health to " + maxHealth);

        if (plugin.getConfig().getBoolean("heal.feed-player", true)) {
            target.setFoodLevel(20);
            target.setSaturation(20);
            plugin.debug("Fed player to full saturation");
        }

        target.setFireTicks(0);

        if (plugin.getConfig().getBoolean("heal.clear-negative-effects", true)) {
            var badEffects = target.getActivePotionEffects().stream()
                    .filter(e -> isNegativeEffect(e.getType()))
                    .toList();

            badEffects.forEach(e -> target.removePotionEffect(e.getType()));
            plugin.debug("Cleared " + badEffects.size() + " negative effects");
        }

        if (target == player) {
            player.sendMessage(lang.get(player, "heal.success"));
        } else {
            Map<String, String> senderPlaceholders = new HashMap<>();
            senderPlaceholders.put("player", target.getName());
            player.sendMessage(lang.get(player, "heal.success.others", senderPlaceholders));

            Map<String, String> targetPlaceholders = new HashMap<>();
            targetPlaceholders.put("healer", player.getName());
            target.sendMessage(lang.get(target, "heal.success.by", targetPlaceholders));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("essentialsc.heal.others")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }

    private boolean isNegativeEffect(PotionEffectType type) {
        return type == PotionEffectType.POISON
                || type == PotionEffectType.WITHER
                || type == PotionEffectType.BLINDNESS
                || type == PotionEffectType.SLOWNESS
                || type == PotionEffectType.HUNGER
                || type == PotionEffectType.WEAKNESS
                || type == PotionEffectType.UNLUCK
                || type == PotionEffectType.MINING_FATIGUE;
    }
}