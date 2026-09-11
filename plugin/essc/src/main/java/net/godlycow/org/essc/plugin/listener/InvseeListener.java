package net.godlycow.org.essc.plugin.listener;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.InventoryViewCompat;
import net.godlycow.org.essc.command.inv.InvseeHolder;
import net.godlycow.org.essc.util.InventorySerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InvseeListener implements Listener {

    private static final int STORAGE_SIZE = InventorySerializer.storageSize();

    private final EssentialsC plugin;
    private final Set<UUID> openOfflineSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Inventory> onlineSessionGuis = new ConcurrentHashMap<>();

    public InvseeListener(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public void registerOfflineSession(UUID targetUuid) {
        openOfflineSessions.add(targetUuid);
    }

    public void registerOnlineSession(UUID targetUuid, Inventory gui) {
        onlineSessionGuis.put(targetUuid, gui);
    }

    public boolean hasOpenOfflineSession(UUID targetUuid) {
        return openOfflineSessions.contains(targetUuid);
    }

    public void closeSessionsForPlayer(UUID targetUuid, Player rejoiningPlayer) {
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            Inventory open = InventoryViewCompat.getTopInventory(viewer);

            if (!(open.getHolder() instanceof InvseeHolder holder)) continue;
            if (!holder.isOffline()) continue;
            if (!holder.getTargetUuid().equals(targetUuid)) continue;

            ItemStack[] slots = extractSlotsFromGui(open);
            openOfflineSessions.remove(targetUuid);
            viewer.closeInventory();

            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                if (!rejoiningPlayer.isOnline()) return;

                InventorySerializer.applyToInventory(slots, rejoiningPlayer.getInventory());
                rejoiningPlayer.updateInventory();

                plugin.debug("Applied offline invsee changes to " + rejoiningPlayer.getName() + " on rejoin");
            });

            if (plugin.getUserManager() != null) {
                plugin.getUserManager().deleteInventory(targetUuid);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player target)) return;

        Inventory gui = onlineSessionGuis.get(target.getUniqueId());
        if (gui == null) return;

        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> syncLivePlayerToGui(target, gui));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!(event.getInventory().getHolder() instanceof InvseeHolder holder)) return;

        int rawSlot = event.getRawSlot();

        boolean isInTopInventory = rawSlot >= 0 && rawSlot < 54;
        boolean isDisplaySlot = rawSlot >= STORAGE_SIZE && rawSlot < 54;

        if (isDisplaySlot) {
            event.setCancelled(true);
            return;
        }

        boolean isShiftClick = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY;

        if ((isInTopInventory || isShiftClick) && !viewer.hasPermission("essentialsc.invsee.modify")) {
            event.setCancelled(true);
            viewer.sendMessage(plugin.getLanguageManager().get(viewer, "invsee.no_modify"));
            return;
        }

        if (!holder.isOffline() && holder.getLiveTarget() != null) {
            Player target = holder.getLiveTarget();

            if (!target.isOnline()) {
                event.setCancelled(true);
                return;
            }

            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> syncGuiToLivePlayer(holder.getInventory(), target));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!(event.getInventory().getHolder() instanceof InvseeHolder holder)) return;

        boolean affectsDisplaySlots = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= STORAGE_SIZE && slot < 54);

        if (affectsDisplaySlots) {
            event.setCancelled(true);
            return;
        }

        boolean affectsTopInventory = event.getRawSlots().stream()
                .anyMatch(slot -> slot < 54);

        if (affectsTopInventory && !viewer.hasPermission("essentialsc.invsee.modify")) {
            event.setCancelled(true);
            viewer.sendMessage(plugin.getLanguageManager().get(viewer, "invsee.no_modify"));
            return;
        }

        if (!holder.isOffline() && holder.getLiveTarget() != null) {
            Player target = holder.getLiveTarget();

            if (!target.isOnline()) {
                event.setCancelled(true);
                return;
            }

            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> syncGuiToLivePlayer(holder.getInventory(), target));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof InvseeHolder holder)) return;

        if (holder.isOffline()) {
            openOfflineSessions.remove(holder.getTargetUuid());
            persistOfflineGui(holder);
            return;
        }

        onlineSessionGuis.remove(holder.getTargetUuid());

        Player target = holder.getLiveTarget();

        if (target != null && target.isOnline()) {
            syncGuiToLivePlayer(holder.getInventory(), target);
        }
    }

    private void syncGuiToLivePlayer(Inventory gui, Player target) {
        ItemStack[] storage = new ItemStack[STORAGE_SIZE];

        for (int i = 0; i < STORAGE_SIZE; i++) {
            storage[i] = gui.getItem(i);
        }

        target.getInventory().setStorageContents(storage);
        target.updateInventory();
    }

    private void syncLivePlayerToGui(Player target, Inventory gui) {
        ItemStack[] storage = target.getInventory().getStorageContents();

        for (int i = 0; i < Math.min(storage.length, STORAGE_SIZE); i++) {
            gui.setItem(i, storage[i]);
        }
    }

    private ItemStack[] extractSlotsFromGui(Inventory gui) {
        ItemStack[] slots = new ItemStack[InventorySerializer.storageSize() + 5];

        for (int i = 0; i < InventorySerializer.storageSize(); i++) {
            slots[i] = gui.getItem(i);
        }

        slots[InventorySerializer.indexHelmet()] = safeArmorSlot(gui.getItem(48));
        slots[InventorySerializer.indexChestplate()] = safeArmorSlot(gui.getItem(47));
        slots[InventorySerializer.indexLeggings()] = safeArmorSlot(gui.getItem(46));
        slots[InventorySerializer.indexBoots()] = safeArmorSlot(gui.getItem(45));
        slots[InventorySerializer.indexOffhand()] = safeArmorSlot(gui.getItem(49));

        return slots;
    }

    private ItemStack safeArmorSlot(ItemStack item) {
        if (item == null || item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return null;
        }

        return item;
    }

    private void persistOfflineGui(InvseeHolder holder) {
        if (plugin.getUserManager() == null) return;

        ItemStack[] slots = extractSlotsFromGui(holder.getInventory());
        String base64 = InventorySerializer.serializeSlots(slots);

        plugin.getUserManager().saveInventory(holder.getTargetUuid(), base64);
        plugin.debug("Persisted modified offline inventory for " + holder.getTargetName());
    }
}