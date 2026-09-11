package net.godlycow.org.essc.integration.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.godlycow.org.essc.EssentialsC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderHook extends PlaceholderExpansion implements Listener {

    private final EssentialsC plugin;
    private final VanishPlaceholders vanishPlaceholders;
    private final HomePlaceholders homePlaceholders;
    private final ShopPlaceholders shopPlaceholders;
    private final RTPPlaceholders rtpPlaceholders;
    private final NickPlaceholders nickPlaceholders;
    private final EconomyPlaceholders economyPlaceholders;
    private final AFKPlaceholders afkPlaceholders;
    private final TPAPlaceholders tpaPlaceholders;
    private final WarpPlaceholders warpPlaceholders;
    private final KitPlaceholders kitPlaceholders;
    private final AuctionPlaceholders auctionPlaceholders;
    private final PunishmentPlaceholders punishmentPlaceholders;
    private final PlaytimePlaceholders playtimePlaceholders;

    public PlaceholderHook(EssentialsC plugin) {
        this.plugin = plugin;
        this.vanishPlaceholders = new VanishPlaceholders(plugin);
        this.homePlaceholders = new HomePlaceholders(plugin);
        this.shopPlaceholders = new ShopPlaceholders(plugin);
        this.rtpPlaceholders = new RTPPlaceholders(plugin);
        this.nickPlaceholders = new NickPlaceholders(plugin);
        this.economyPlaceholders = new EconomyPlaceholders(plugin);
        this.afkPlaceholders = new AFKPlaceholders(plugin);
        this.tpaPlaceholders = new TPAPlaceholders(plugin);
        this.warpPlaceholders = new WarpPlaceholders(plugin);
        this.kitPlaceholders = new KitPlaceholders(plugin);
        this.auctionPlaceholders = new AuctionPlaceholders(plugin);
        this.punishmentPlaceholders = new PunishmentPlaceholders(plugin);
        this.playtimePlaceholders = new PlaytimePlaceholders(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "essc";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty()
                ? "_GodlyCow"
                : String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        String result;

        result = vanishPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = homePlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = shopPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = rtpPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = nickPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = economyPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = afkPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = tpaPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = warpPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = kitPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = auctionPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = punishmentPlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        result = playtimePlaceholders.onRequest(player, identifier);
        if (result != null) return result;

        return null;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        homePlaceholders.clearCache(event.getPlayer().getUniqueId());
        shopPlaceholders.clearCache(event.getPlayer().getUniqueId());
        economyPlaceholders.clearCache(event.getPlayer().getUniqueId());
    }

    public static List<String> getAllPlaceholders() {
        List<String> placeholders = new ArrayList<>();

        placeholders.addAll(VanishPlaceholders.getPlaceholderList());
        placeholders.addAll(HomePlaceholders.getPlaceholderList());
        placeholders.addAll(ShopPlaceholders.getPlaceholderList());
        placeholders.addAll(RTPPlaceholders.getPlaceholderList());
        placeholders.addAll(NickPlaceholders.getPlaceholderList());
        placeholders.addAll(EconomyPlaceholders.getPlaceholderList());
        placeholders.addAll(AFKPlaceholders.getPlaceholderList());
        placeholders.addAll(TPAPlaceholders.getPlaceholderList());
        placeholders.addAll(WarpPlaceholders.getPlaceholderList());
        placeholders.addAll(KitPlaceholders.getPlaceholderList());
        placeholders.addAll(AuctionPlaceholders.getPlaceholderList());
        placeholders.addAll(PunishmentPlaceholders.getPlaceholderList());
        placeholders.addAll(PlaytimePlaceholders.getPlaceholderList());

        return placeholders;
    }
}