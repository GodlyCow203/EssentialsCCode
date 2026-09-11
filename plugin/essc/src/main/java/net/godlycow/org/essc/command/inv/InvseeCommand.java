package net.godlycow.org.essc.command.inv;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.plugin.listener.InvseeListener;
import net.godlycow.org.essc.util.InventorySerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvseeCommand extends Command {

    public InvseeCommand(EssentialsC plugin) {
        super(plugin, "invsee", "essentialsc.invsee", true, 1, "command.usage.invsee");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        Player target = plugin.getServer().getPlayer(args[0]);

        if (target == null) {
            OfflinePlayer offlineTarget = plugin.getServer().getOfflinePlayer(args[0]);

            if (!offlineTarget.hasPlayedBefore()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[0]);
                player.sendMessage(lang.get(player, "error.player_not_found", placeholders));
                return true;
            }

            if (!player.hasPermission("essentialsc.invsee.offline")) {
                player.sendMessage(lang.get(player, "error.no_permission"));
                return true;
            }

            String targetName = offlineTarget.getName() != null ? offlineTarget.getName() : args[0];

            if (plugin.getUserManager() == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("target", targetName);
                player.sendMessage(lang.get(player, "error.player_offline_no_inventory", placeholders));
                return true;
            }

            plugin.getUserManager().loadInventory(offlineTarget.getUniqueId()).thenAccept(base64 -> {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                    if (!player.isOnline()) return;

                    Player nowOnline = plugin.getServer().getPlayer(offlineTarget.getUniqueId());
                    if (nowOnline != null) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("target", targetName);
                        player.sendMessage(lang.get(player, "invsee.target_now_online", placeholders));
                        openOnlineGui(player, nowOnline);

                        Map<String, String> openedPlaceholders = new HashMap<>();
                        openedPlaceholders.put("target", targetName);
                        player.sendMessage(lang.get(player, "invsee.opened", openedPlaceholders));
                        return;
                    }

                    if (base64 == null) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("target", targetName);
                        player.sendMessage(lang.get(player, "error.player_offline_no_inventory", placeholders));
                        return;
                    }

                    ItemStack[] slots = InventorySerializer.deserialize(base64);
                    openOfflineGui(player, offlineTarget.getUniqueId(), targetName, slots);

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("target", targetName);
                    player.sendMessage(lang.get(player, "invsee.opened_offline", placeholders));
                    plugin.debug(player.getName() + " is viewing " + targetName + "'s inventory (offline)");
                });
            });

            return true;
        }

        if (target == player) {
            player.sendMessage(lang.get(player, "invsee.self"));
            return true;
        }

        openOnlineGui(player, target);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("target", target.getName());
        player.sendMessage(lang.get(player, "invsee.opened", placeholders));
        plugin.debug(player.getName() + " is viewing " + target.getName() + "'s inventory");
        return true;
    }

    private void openOnlineGui(Player viewer, Player target) {
        InvseeHolder holder = new InvseeHolder(target.getUniqueId(), target.getName(), false);
        holder.setLiveTarget(target);

        Component title = plugin.getMiniMessage().deserialize("<gray>" + target.getName() + "'s Inventory");
        Inventory gui = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(gui);

        populateFromLiveInventory(gui, target.getInventory());
        fillBottomRow(gui);
        viewer.openInventory(gui);

        getInvseeListener().registerOnlineSession(target.getUniqueId(), gui);
    }

    private void openOfflineGui(Player viewer, java.util.UUID targetUuid, String targetName, ItemStack[] slots) {
        InvseeHolder holder = new InvseeHolder(targetUuid, targetName, true);

        Component title = plugin.getMiniMessage().deserialize(
                "<gray>" + targetName + "'s Inventory <dark_gray>(offline)</dark_gray>"
        );
        Inventory gui = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(gui);

        int storageSize = InventorySerializer.storageSize();
        for (int i = 0; i < storageSize && i < slots.length; i++) {
            gui.setItem(i, slots[i]);
        }

        if (slots.length > InventorySerializer.indexHelmet()) {
            gui.setItem(48, slots[InventorySerializer.indexHelmet()]);
        }
        if (slots.length > InventorySerializer.indexChestplate()) {
            gui.setItem(47, slots[InventorySerializer.indexChestplate()]);
        }
        if (slots.length > InventorySerializer.indexLeggings()) {
            gui.setItem(46, slots[InventorySerializer.indexLeggings()]);
        }
        if (slots.length > InventorySerializer.indexBoots()) {
            gui.setItem(45, slots[InventorySerializer.indexBoots()]);
        }
        if (slots.length > InventorySerializer.indexOffhand()) {
            gui.setItem(49, slots[InventorySerializer.indexOffhand()]);
        }

        fillBottomRow(gui);
        viewer.openInventory(gui);

        getInvseeListener().registerOfflineSession(targetUuid);
    }

    private InvseeListener getInvseeListener() {
        return plugin.getInvseeListener();
    }

    private void populateFromLiveInventory(Inventory gui, PlayerInventory source) {
        ItemStack[] storage = source.getStorageContents();

        for (int i = 0; i < Math.min(storage.length, InventorySerializer.storageSize()); i++) {
            gui.setItem(i, storage[i]);
        }

        gui.setItem(48, source.getHelmet());
        gui.setItem(47, source.getChestplate());
        gui.setItem(46, source.getLeggings());
        gui.setItem(45, source.getBoots());

        ItemStack offhand = source.getItemInOffHand();
        gui.setItem(49, offhand.getType() == Material.AIR ? null : offhand);
    }

    private void fillBottomRow(Inventory gui) {
        ItemStack filler = buildFillerPane();

        for (int i = InventorySerializer.storageSize(); i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }

    private ItemStack buildFillerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
        pane.setItemMeta(meta);
        return pane;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equals(sender.getName()))
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}