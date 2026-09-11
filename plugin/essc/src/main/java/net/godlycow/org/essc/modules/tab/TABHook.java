package net.godlycow.org.essc.modules.tab;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import net.godlycow.org.essc.EssentialsC;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import org.bukkit.entity.Player;

public class TABHook {

    private final EssentialsC plugin;

    public TABHook(EssentialsC plugin) {
        this.plugin = plugin;
        plugin.debug("TABHook initialized");
    }

    public void updateNick(Player player, String nickname) {
        if (TabAPI.getInstance() == null) {
            plugin.debug("TABHook: TabAPI not available yet");
            return;
        }
        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        if (tabPlayer == null) {
            plugin.debug("TABHook: TabPlayer not found for " + player.getName());
            return;
        }

        String display = buildDisplay(player, nickname);
        setTablistName(tabPlayer, display);
    }

    private void setTablistName(TabPlayer tabPlayer, String display) {
        TabListFormatManager manager = TabAPI.getInstance().getTabListFormatManager();
        if (manager == null) {
            plugin.debug("TABHook: TabListFormatManager is null (tablist-name-formatting disabled in TAB?)");
            return;
        }
        manager.setName(tabPlayer, display);
    }

    private String buildDisplay(Player player, String nickname) {
        StringBuilder display = new StringBuilder();

        if (plugin.getAfkManager() != null
                && plugin.getAfkManager().isAFK(player)
                && plugin.getConfigManager().isAfkTabPlaceholderEnabled()) {

            String afkTag = plugin.getConfigManager().getAfkTabPlaceholder();
            if (afkTag != null && !afkTag.isBlank()) {
                display.append(afkTag);
            }
        }

        if (nickname != null && !nickname.isBlank()) {
            display.append(nickname);
        } else {
            display.append(player.getName());
        }

        return display.toString();
    }
}