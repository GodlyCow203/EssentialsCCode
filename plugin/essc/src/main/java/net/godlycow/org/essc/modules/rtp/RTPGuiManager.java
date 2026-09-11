package net.godlycow.org.essc.modules.rtp;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;



import java.util.*;

public class RTPGuiManager implements Listener {

    private final EssentialsC plugin;
    private final RTPManager rtpManager;
    private final MiniMessage miniMessage;

    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private final Map<UUID, ScheduledTask> updateTasks = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private static final int[] WORLD_SLOTS = {11, 13, 15};

    private static final int SLOT_PREV = 18;
    private static final int SLOT_PAGE = 22;
    private static final int SLOT_NEXT = 26;

    private static final int WORLDS_PER_PAGE = 3;

    public RTPGuiManager(EssentialsC plugin, RTPManager rtpManager) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.miniMessage = plugin.getMiniMessage();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {

        if (!player.hasPermission("essentialsc.rtp")) {
            player.sendMessage(plugin.getLanguageManager().get(player, "rtp.error.no_permission"));
            return;
        }

        playerPages.put(player.getUniqueId(), 0);

        Component title = plugin.getLanguageManager().get(player, "rtp.gui.title");
        Inventory inv = Bukkit.createInventory(null, 27, title);
        openInventories.put(player.getUniqueId(), inv);

        refreshInventory(player);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);

        startUpdateTask(player);
    }

    private void refreshInventory(Player player) {

        Inventory inv = openInventories.get(player.getUniqueId());
        if (inv == null) return;

        List<String> worlds = rtpManager.getConfiguredWorldNames();
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
        int totalPages = Math.max(1, (int) Math.ceil((double) worlds.size() / WORLDS_PER_PAGE));

        if (page >= totalPages) {
            page = totalPages - 1;
            playerPages.put(player.getUniqueId(), page);
        }

        ItemStack filler = createFiller();

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        int pageStart = page * WORLDS_PER_PAGE;
        for (int i = 0; i < WORLDS_PER_PAGE; i++) {
            int worldIndex = pageStart + i;
            if (worldIndex < worlds.size()) {
                inv.setItem(WORLD_SLOTS[i], buildWorldItem(worlds.get(worldIndex), player));
            }
        }

        if (page > 0) {
            inv.setItem(SLOT_PREV, buildNavButton(false, player));
        }
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, buildNavButton(true, player));
        }

        inv.setItem(SLOT_PAGE, buildPageIndicator(page + 1, totalPages, player));
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildWorldItem(String worldName, Player viewer) {

        World world = rtpManager.resolveWorld(worldName);
        if (world == null) return createFiller();

        RTPManager.WorldRTPSettings settings = rtpManager.getWorldSettings(worldName);
        boolean enabled = rtpManager.isWorldEnabled(worldName);
        boolean hasPermission = rtpManager.hasWorldPermission(viewer, worldName);
        int playerCount = rtpManager.getPlayerCountInWorld(worldName);

        Material material = enabled && hasPermission
                ? materialForEnvironment(world.getEnvironment())
                : Material.BARRIER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        Component displayName = miniMessage.deserialize(settings.displayName())
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(displayName);

        List<Component> lore = new ArrayList<>();

        if (!hasPermission) {
            lore.add(lang(viewer, "rtp.gui.status.no_permission"));
        } else if (enabled) {
            lore.add(lang(viewer, "rtp.gui.status.enabled"));
        } else {
            lore.add(lang(viewer, "rtp.gui.status.disabled"));
        }

        lore.add(lang(viewer, "rtp.gui.players", map("count", String.valueOf(playerCount), "world", worldName)));

        if (!hasPermission) {
            lore.add(lang(viewer, "rtp.gui.locked"));
        } else if (enabled) {
            lore.add(lang(viewer, "rtp.gui.click_to_teleport"));
        } else {
            lore.add(lang(viewer, "rtp.gui.world_disabled_lore"));
        }

        lore.add(lang(viewer, "rtp.gui.radius",
                map("min", String.valueOf(settings.minRadius()), "max", String.valueOf(settings.maxRadius()))));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavButton(boolean forward, Player viewer) {

        ItemStack item = new ItemStack(forward ? Material.ARROW : Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        Component name = forward
                ? lang(viewer, "rtp.gui.nav.next")
                : lang(viewer, "rtp.gui.nav.prev");

        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPageIndicator(int current, int total, Player viewer) {

        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        Component name = lang(viewer, "rtp.gui.nav.page",
                map("page", String.valueOf(current), "total", String.valueOf(total)));

        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of());
        item.setItemMeta(meta);
        return item;
    }

    private Material materialForEnvironment(World.Environment env) {
        return switch (env) {
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }

    private Component lang(Player player, String key) {
        return plugin.getLanguageManager().get(player, key)
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component lang(Player player, String key, Map<String, String> placeholders) {
        return plugin.getLanguageManager().get(player, key, placeholders)
                .decoration(TextDecoration.ITALIC, false);
    }

    private Map<String, String> map(String k1, String v1, String k2, String v2) {
        Map<String, String> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private Map<String, String> map(String k1, String v1) {
        return Map.of(k1, v1);
    }


    private void startUpdateTask(Player player) {
        UUID uuid = player.getUniqueId();

        ScheduledTask old = updateTasks.remove(uuid);
        if (old != null) old.cancel();

        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, task1 -> {
            if (!player.isOnline() || !openInventories.containsKey(uuid)) {
                ScheduledTask t = updateTasks.remove(uuid);
                if (t != null) t.cancel();
                return;
            }
            refreshInventory(player);
        }, null, 20L, 20L);

        updateTasks.put(uuid, task);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = openInventories.get(player.getUniqueId());
        if (inv == null || event.getInventory() != inv) return;

        event.setCancelled(true);

        int slot = event.getSlot();

        if (slot == SLOT_PREV || slot == SLOT_NEXT) {
            handlePageTurn(player, slot == SLOT_NEXT);
            return;
        }

        int slotIndex = worldSlotIndex(slot);
        if (slotIndex == -1) return;

        List<String> worlds = rtpManager.getConfiguredWorldNames();
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
        int worldIndex = page * WORLDS_PER_PAGE + slotIndex;

        if (worldIndex >= worlds.size()) return;

        String worldName = worlds.get(worldIndex);
        World world = rtpManager.resolveWorld(worldName);

        if (world == null) {
            player.sendMessage(plugin.getLanguageManager().get(player, "rtp.error.world_not_found"));
            return;
        }

        if (!rtpManager.hasWorldPermission(player, worldName) || !rtpManager.isWorldEnabled(worldName)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        player.closeInventory();
        rtpManager.startRTP(player, world);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (!(event.getPlayer() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        if (!openInventories.containsKey(uuid)) return;

        openInventories.remove(uuid);
        playerPages.remove(uuid);

        ScheduledTask task = updateTasks.remove(uuid);
        if (task != null) task.cancel();

        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1f, 1f);
    }

    private void handlePageTurn(Player player, boolean forward) {

        List<String> worlds = rtpManager.getConfiguredWorldNames();
        int totalPages = Math.max(1, (int) Math.ceil((double) worlds.size() / WORLDS_PER_PAGE));

        int current = playerPages.getOrDefault(player.getUniqueId(), 0);
        int next = forward ? current + 1 : current - 1;

        if (next < 0 || next >= totalPages) return;

        playerPages.put(player.getUniqueId(), next);
        refreshInventory(player);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, forward ? 1.2f : 0.8f);
    }
    public void shutdown() {
        updateTasks.values().forEach(ScheduledTask::cancel);
        updateTasks.clear();
        openInventories.clear();
        playerPages.clear();
    }

    private int worldSlotIndex(int slot) {
        for (int i = 0; i < WORLD_SLOTS.length; i++) {
            if (WORLD_SLOTS[i] == slot) return i;
        }
        return -1;
    }
}