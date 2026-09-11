package net.godlycow.org.essc.modules.kit.gui;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class KitSoundManager {
    private final EssentialsC plugin;

    public KitSoundManager(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public void playOpen(Player player) {
        if (!plugin.getConfigManager().isKitGuiSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.0f);
    }

    public void playPageTurn(Player player) {
        if (!plugin.getConfigManager().isKitGuiSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.0f);
    }

    public void playClaim(Player player) {
        if (!plugin.getConfigManager().isKitGuiSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
    }

    public void playDenied(Player player) {
        if (!plugin.getConfigManager().isKitGuiSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
    }

    public void playClose(Player player) {
        if (!plugin.getConfigManager().isKitGuiSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.6f, 1.0f);
    }
}
