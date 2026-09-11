package net.godlycow.org.essc.modules.kit.gui;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.kit.Kit;
import net.godlycow.org.essc.plugin.gui.GuiFramework;
import net.godlycow.org.essc.plugin.gui.GuiTemplate;
import net.godlycow.org.essc.plugin.gui.GuiButton;
import net.godlycow.org.essc.util.ComponentHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KitGuiManager {

    private final EssentialsC plugin;
    private final GuiFramework guiFramework;
    private final KitSoundManager sounds;
    private final Map<UUID, Integer> activeSessions = new ConcurrentHashMap<>();

    public KitGuiManager(EssentialsC plugin, GuiFramework guiFramework) {
        this.plugin = plugin;
        this.guiFramework = guiFramework;
        this.sounds = new KitSoundManager(plugin);

        plugin.getServer().getPluginManager().registerEvents(
                new KitGuiListener(plugin, this), plugin);
    }

    public void openKitList(Player player, int page) {
        GuiTemplate template = guiFramework.getTemplate("kit_list");
        if (template == null) {
            plugin.getLogger().warning("[KitGUI] Missing GUI template: " + "kit_list" + ".yml");
            return;
        }

        List<Integer> kitSlots = templateSlotList(template, "kit-slots");
        int perPage = Math.max(1, kitSlots.size());

        List<Kit> allKits = new ArrayList<>(plugin.getKitManager().getKits());

        List<Kit> autoKits = new ArrayList<>();
        Map<Integer, Map<Integer, Kit>> pinnedKits = new HashMap<>();

        int maxPage = 1;
        for (Kit kit : allKits) {
            maxPage = Math.max(maxPage, kit.getGuiPage());
        }

        for (Kit kit : allKits) {
            int slot = kit.getGuiSlot();
            if (slot >= 0 && slot < template.getSize()) {
                pinnedKits.computeIfAbsent(kit.getGuiPage(), p -> new HashMap<>()).put(slot, kit);
            } else {
                autoKits.add(kit);
            }
        }

        int totalPages = Math.max(1, Math.max(maxPage,
                (int) Math.ceil((double) autoKits.size() / perPage)));
        int safePage = Math.max(1, Math.min(page, totalPages));

        Component title = template.resolveTitle(player, plugin, Map.of("page", String.valueOf(safePage), "total", String.valueOf(totalPages)));

        KitGuiHolder holder = new KitGuiHolder(safePage);
        Inventory gui = Bukkit.createInventory(holder, template.getSize(), title);
        holder.setInventory(gui);

        guiFramework.fillStaticItems(gui, "kit_list", player);

        GuiButton emptyBtn = template.getItem("empty");

        int start = (safePage - 1) * perPage;
        int end = Math.min(start + perPage, autoKits.size());

        Map<Integer, Kit> pagePinned = pinnedKits.get(safePage);
        if (pagePinned != null) {
            for (Map.Entry<Integer, Kit> entry : pagePinned.entrySet()) {
                int slot = entry.getKey();
                Kit kit = entry.getValue();
                if (plugin.getKitManager().hasPermission(player, kit)) {
                    gui.setItem(slot, buildKitItem(player, kit));
                } else if (emptyBtn != null) {
                    gui.setItem(slot, guiFramework.getItemBuilder().build(emptyBtn, player));
                }
            }
        }

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex < kitSlots.size()) {
                int targetSlot = kitSlots.get(slotIndex);
                Kit pinned = pagePinned != null ? pagePinned.get(targetSlot) : null;
                if (pinned != null) continue;
                Kit kit = autoKits.get(i);
                if (plugin.getKitManager().hasPermission(player, kit)) {
                    gui.setItem(targetSlot, buildKitItem(player, kit));
                } else if (emptyBtn != null) {
                    gui.setItem(targetSlot, guiFramework.getItemBuilder().build(emptyBtn, player));
                }
            }
        }

        GuiButton navPrev = template.getItem("nav-prev");
        GuiButton navNext = template.getItem("nav-next");

        if (navPrev != null && !navPrev.getSlots().isEmpty()) {
            int slot = navPrev.getSlots().get(0);
            gui.setItem(slot, guiFramework.getItemBuilder().build(navPrev, player));
        }

        if (navNext != null && !navNext.getSlots().isEmpty()) {
            int slot = navNext.getSlots().get(0);
            gui.setItem(slot, guiFramework.getItemBuilder().build(navNext, player));
        }

        activeSessions.put(player.getUniqueId(), safePage);
        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                player.openInventory(gui);
                sounds.playOpen(player);
            }
        }, null);
    }

    public void handleKitClick(Player player, String kitName, int returnPage) {
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) return;

        openKitPreview(player, kit, returnPage);
    }

    public void openKitPreview(Player player, Kit kit, int returnPage) {
        GuiTemplate template = guiFramework.getTemplate("kit_preview");
        if (template == null) {
            plugin.getLogger().warning("[KitGUI] Missing GUI template: kit_preview.yml");
            return;
        }

        Component title = template.resolveTitle(player, plugin, Map.of(
                "kit", kit.getDisplayName(), "name", kit.getName()));

        KitGuiHolder holder = new KitGuiHolder(0, returnPage, kit.getName());
        Inventory gui = Bukkit.createInventory(holder, template.getSize(), title);
        holder.setInventory(gui);

        guiFramework.fillStaticItems(gui, "kit_preview", player);

        List<Integer> previewSlots = templateSlotList(template, "preview-slot");

        List<ItemStack> items = kit.getItems();
        for (int i = 0; i < previewSlots.size() && i < items.size(); i++) {
            gui.setItem(previewSlots.get(i), items.get(i).clone());
        }

        GuiButton backBtn = template.getItem("back");
        if (backBtn != null && !backBtn.getSlots().isEmpty()) {
            gui.setItem(backBtn.getSlots().get(0), guiFramework.getItemBuilder().build(backBtn, player));
        }

        GuiButton claimBtn = template.getItem("claim");
        if (claimBtn != null && !claimBtn.getSlots().isEmpty()) {
            gui.setItem(claimBtn.getSlots().get(0), buildClaimButton(player, kit, claimBtn));
        }

        activeSessions.put(player.getUniqueId(), returnPage);
        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                player.openInventory(gui);
                sounds.playOpen(player);
            }
        }, null);
    }

    private ItemStack buildClaimButton(Player player, Kit kit, GuiButton config) {
        boolean canClaim = plugin.getKitManager().canClaim(player, kit);
        boolean oneTimeUsed = kit.isOneTime() && plugin.getKitManager().hasClaimed(player, kit);

        String nameKey = canClaim
                ? "kit.preview.item.claim.name"
                : (oneTimeUsed ? "kit.preview.item.claim.name.used" : "kit.preview.item.claim.name.cooldown");

        List<Component> lore = new ArrayList<>();
        if (canClaim) {
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player,
                    "kit.preview.item.claim.lore.click")));
        } else {
            long remaining = plugin.getKitManager().getCooldownRemaining(player, kit);
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player,
                    "kit.preview.item.claim.lore.ready_in",
                    Map.of("time", formatTime(Math.max(0, remaining))))));
        }

        Material material;
        if (canClaim) {
            material = config.getAvailableMaterial() != null
                    ? config.getAvailableMaterial()
                    : Material.LIME_CONCRETE;
        } else {
            material = config.getUnavailableMaterial() != null
                    ? config.getUnavailableMaterial()
                    : Material.RED_CONCRETE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(ComponentHelper.noItalic(
                plugin.getLanguageManager().get(player, nameKey, Map.of("kit", kit.getDisplayName()))));
        meta.lore(ComponentHelper.noItalic(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        NamespacedKey actionKey = new NamespacedKey(plugin, "gui_action");
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "kit_preview_claim");
        NamespacedKey kitKey = new NamespacedKey(plugin, "kit_name");
        meta.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, kit.getName());

        item.setItemMeta(meta);
        return item;
    }

    public void handlePageTurn(Player player, int newPage) {
        sounds.playPageTurn(player);
        openKitList(player, newPage);
    }

    public void handlePreviewBack(Player player, int returnPage) {
        sounds.playPageTurn(player);
        openKitList(player, returnPage);
    }

    public void handlePreviewClaim(Player player, String kitName, int returnPage) {
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            openKitList(player, returnPage);
            return;
        }

        if (!plugin.getKitManager().hasPermission(player, kit)) {
            sounds.playDenied(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "kit.no_permission",
                    Map.of("kit", kit.getDisplayName())));
            return;
        }

        plugin.getKitManager().getCooldownRemainingAsync(player, kit).thenAccept(cooldown -> {
            player.getScheduler().run(plugin, task -> {
                if (!player.isOnline()) return;

                if (!plugin.getKitManager().canClaim(player, kit)) {
                    sounds.playDenied(player);

                    if (kit.isOneTime() && plugin.getKitManager().hasClaimed(player, kit)) {
                        player.sendMessage(plugin.getLanguageManager().get(player, "kit.one_time_used",
                                Map.of("kit", kit.getDisplayName())));
                        return;
                    }

                    if (kit.getMaxClaims() > 0) {
                        int count = plugin.getKitManager().getClaimCount(player, kit);
                        if (count >= kit.getMaxClaims()) {
                            player.sendMessage(plugin.getLanguageManager().get(player, "kit.max_claims_reached",
                                    Map.of("kit", kit.getDisplayName(), "max", String.valueOf(kit.getMaxClaims()))));
                            return;
                        }
                    }

                    if (cooldown > 0 && !plugin.getKitManager().hasCooldownBypass(player, kit)) {
                        player.sendMessage(plugin.getLanguageManager().get(player, "kit.cooldown_active",
                                Map.of("kit", kit.getDisplayName(), "time", formatTime(cooldown))));
                        return;
                    }

                    return;
                }

                if (cooldown > 0 && !plugin.getKitManager().hasCooldownBypass(player, kit)) {
                    sounds.playDenied(player);
                    player.sendMessage(plugin.getLanguageManager().get(player, "kit.cooldown_active",
                            Map.of("kit", kit.getDisplayName(), "time", formatTime(cooldown))));
                    return;
                }

                sounds.playClaim(player);
                plugin.getKitManager().giveKit(player, kit);
                if (player.isOnline()) {
                    player.closeInventory();
                }
            }, null);
        });
    }

    public void handleClose(Player player) {
        sounds.playClose(player);
    }

    public void clearSession(UUID uuid) {
        activeSessions.remove(uuid);
    }

    private List<Integer> templateSlotList(GuiTemplate template, String itemId) {
        GuiButton button = template.getItem(itemId);
        if (button == null) {
            return List.of();
        }
        return new ArrayList<>(button.getSlots());
    }

    private ItemStack buildKitItem(Player player, Kit kit) {
        boolean canClaim = plugin.getKitManager().canClaim(player, kit);
        boolean onCooldown = !canClaim && kit.getCooldown() > 0;
        boolean oneTimeUsed = kit.isOneTime() && plugin.getKitManager().hasClaimed(player, kit);

        Material material = resolveIcon(kit, canClaim);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Component name;
        if (canClaim) {
            name = plugin.getLanguageManager().get(player, "kit.gui.item.kit.name.available",
                    Map.of("kit", kit.getDisplayName()));
        } else if (oneTimeUsed) {
            name = plugin.getLanguageManager().get(player, "kit.gui.item.kit.name.used",
                    Map.of("kit", kit.getDisplayName()));
        } else {
            name = plugin.getLanguageManager().get(player, "kit.gui.item.kit.name.cooldown",
                    Map.of("kit", kit.getDisplayName()));
        }

        meta.displayName(ComponentHelper.noItalic(name));

        List<Component> lore = new ArrayList<>();

        if (!kit.getDescription().isEmpty()) {
            for (String line : kit.getDescription().split("\\n", -1)) {
                if (line.isEmpty()) {
                    lore.add(ComponentHelper.noItalic(Component.empty()));
                    continue;
                }
                lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player,
                        "kit.gui.item.kit.lore.description",
                        Map.of("description", line))));
            }
            lore.add(ComponentHelper.noItalic(Component.empty()));
        }

        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player,
                "kit.gui.item.kit.lore.items",
                Map.of("count", String.valueOf(kit.getItems().size())))));

        if (kit.getCooldown() > 0) {
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player,
                    "kit.gui.item.kit.lore.cooldown",
                    Map.of("time", formatTime(kit.getCooldown())))));
        }

        if (onCooldown) {
            long remaining = plugin.getKitManager().getCooldownRemaining(player, kit);
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player,
                    "kit.gui.item.kit.lore.ready_in",
                    Map.of("time", formatTime(remaining)))));
        }

        if (kit.getMaxClaims() > 0) {
            int claimCount = plugin.getKitManager().getClaimCount(player, kit);
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player,
                    "kit.gui.item.kit.lore.claims",
                    Map.of("count", String.valueOf(claimCount), "max", String.valueOf(kit.getMaxClaims())))));
        }

        if (canClaim) {
            lore.add(ComponentHelper.noItalic(Component.empty()));
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player,
                    "kit.gui.item.kit.lore.click_to_claim")));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        NamespacedKey kitKey = new NamespacedKey(plugin, "kit_name");
        meta.getPersistentDataContainer().set(kitKey, PersistentDataType.STRING, kit.getName());

        item.setItemMeta(meta);
        return item;
    }

    private Material resolveIcon(Kit kit, boolean canClaim) {
        String iconStr = kit.getGuiIcon();
        if (iconStr != null && !iconStr.isEmpty()) {
            Material parsed = Material.matchMaterial(iconStr.toUpperCase());
            if (parsed != null) {
                return parsed;
            }
            plugin.getLogger().warning("[KitGUI] Invalid gui-icon '" + iconStr + "' for kit " + kit.getName() + ", using default.");
        }
        return canClaim ? Material.CHEST : Material.BARREL;
    }

    private String formatTime(long seconds) {
        Duration dur = Duration.ofSeconds(seconds);
        long hours = dur.toHours();
        long mins = dur.toMinutesPart();
        long secs = dur.toSecondsPart();
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, mins, secs);
        }
        if (mins > 0) {
            return String.format("%dm %ds", mins, secs);
        }
        return secs + "s";
    }
}