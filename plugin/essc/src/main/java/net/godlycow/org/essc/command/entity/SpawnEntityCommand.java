package net.godlycow.org.essc.command.entity;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class SpawnEntityCommand extends Command {

    public SpawnEntityCommand(EssentialsC plugin) {
        super(plugin, "spawnentity", "essentialsc.spawnentity", true, 1, "command.usage.spawnentity");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        String entityName = args[0].toUpperCase();
        EntityType entityType;

        try {
            entityType = EntityType.valueOf(entityName);
        } catch (IllegalArgumentException e) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("entity", args[0]);
            player.sendMessage(lang.get(player, "spawnentity.invalid", placeholders));
            return true;
        }

        if (!entityType.isSpawnable()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("entity", entityType.name());
            player.sendMessage(lang.get(player, "spawnentity.not_spawnable", placeholders));
            return true;
        }

        if (!hasEntityPermission(player, entityType)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("entity", entityType.name());
            player.sendMessage(lang.get(player, "spawnentity.no_permission_entity", placeholders));
            return true;
        }

        int amount = 1;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1) {
                    player.sendMessage(lang.get(player, "spawnentity.invalid_amount"));
                    return true;
                }
                int maxAmount = plugin.getConfigManager().getSpawnEntityMaxAmount();
                if (amount > maxAmount && !player.hasPermission("essentialsc.spawnentity.bypasslimit")) {
                    amount = maxAmount;
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("max", String.valueOf(maxAmount));
                    player.sendMessage(lang.get(player, "spawnentity.amount_capped", placeholders));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(lang.get(player, "spawnentity.invalid_amount"));
                return true;
            }
        }

        Location location = player.getLocation();
        int spawned = 0;

        for (int i = 0; i < amount; i++) {
            try {
                player.getWorld().spawnEntity(location, entityType);
                spawned++;
            } catch (Exception e) {
                plugin.debug("Failed to spawn " + entityType.name() + ": " + e.getMessage());
                break;
            }
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(spawned));
        placeholders.put("entity", entityType.name().toLowerCase().replace("_", " "));
        player.sendMessage(lang.get(player, "spawnentity.success", placeholders));

        plugin.debug(player.getName() + " spawned " + spawned + "x " + entityType.name());

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String input = args[0].toUpperCase();
            return Arrays.stream(EntityType.values())
                    .filter(EntityType::isSpawnable)
                    .filter(e -> hasEntityPermission(player, e))
                    .map(EntityType::name)
                    .filter(name -> name.startsWith(input))
                    .map(String::toLowerCase)
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            int maxAmount = plugin.getConfigManager().getSpawnEntityMaxAmount();
            return List.of("1", "2", "3", "5", String.valueOf(maxAmount)).stream()
                    .filter(s -> s.startsWith(args[1]))
                    .toList();
        }

        return Collections.emptyList();
    }

    private boolean hasEntityPermission(Player player, EntityType entityType) {
        if (player.hasPermission("essentialsc.spawnentity.*")) {
            return true;
        }
        String entityPerm = "essentialsc.spawnentity." + entityType.name().toLowerCase();
        return player.hasPermission(entityPerm);
    }
}