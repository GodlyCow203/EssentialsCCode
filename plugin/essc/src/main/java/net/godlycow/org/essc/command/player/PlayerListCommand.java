package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.util.LegacyColorConverter;
import net.godlycow.org.essc.util.PaginatedList;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class PlayerListCommand extends Command {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public PlayerListCommand(EssentialsC plugin) {
        super(plugin, "playerlist", "essentialsc.playerlist", false, 0, "command.usage.playerlist");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        int total = onlinePlayers.size();
        int max = Bukkit.getMaxPlayers();


        if (total == 0) {
            sender.sendMessage(lang.get(sender, "playerlist.empty"));
            return true;
        }

        int requestedPage = 1;
        if (args.length >= 1) {
            try {
                requestedPage = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(lang.get(sender, "playerlist.invalid_page"));
                return true;
            }
        }

        boolean useLuckPerms = plugin.getConfigManager().isPlayerListLuckPermsEnabled()
                && plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null;

        LuckPerms luckPerms = null;
        if (useLuckPerms) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) luckPerms = provider.getProvider();
        }

        List<String> playerDisplays = new ArrayList<>();
        for (Player player : onlinePlayers) {
            String display;
            if (useLuckPerms && luckPerms != null) {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    String suffix = user.getCachedData().getMetaData().getSuffix();

                    StringBuilder sb = new StringBuilder();
                    if (prefix != null) sb.append(LegacyColorConverter.toMiniMessage(prefix));
                    sb.append(player.getName());
                    if (suffix != null) sb.append(LegacyColorConverter.toMiniMessage(suffix));
                    display = sb.toString();
                } else {
                    display = player.getName();
                }
            } else {
                display = player.getName();
            }
            playerDisplays.add(display);
        }

        playerDisplays.sort(String.CASE_INSENSITIVE_ORDER);

        PaginatedList<String> paginated = new PaginatedList<>(playerDisplays, 10);
        int page = paginated.clamp(requestedPage);

        if (!paginated.isValidPage(requestedPage)) {
            sender.sendMessage(lang.get(sender, "playerlist.invalid_page"));
            return true;
        }

        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("online", String.valueOf(total));
        headerPlaceholders.put("max", String.valueOf(max));
        headerPlaceholders.put("page", String.valueOf(page));
        headerPlaceholders.put("total_pages", String.valueOf(paginated.getTotalPages()));
        sender.sendMessage(lang.get(sender, "playerlist.header", headerPlaceholders));

        for (String display : paginated.getPage(page)) {
            sender.sendMessage(MM.deserialize("<color:#AAAAAA>• </color>" + display));
        }

        sender.sendMessage(lang.get(sender, "playerlist.footer", headerPlaceholders));

        if (sender instanceof Player) {
            boolean hasPrev = paginated.hasPreviousPage(page);
            boolean hasNext = paginated.hasNextPage(page);

            if (hasPrev || hasNext) {
                Map<String, String> navPlaceholders = new HashMap<>();
                navPlaceholders.put("prev_page", String.valueOf(page - 1));
                navPlaceholders.put("next_page", String.valueOf(page + 1));
                navPlaceholders.put("has_prev", String.valueOf(hasPrev));
                navPlaceholders.put("has_next", String.valueOf(hasNext));
                navPlaceholders.put("total_pages", String.valueOf(paginated.getTotalPages()));
                sender.sendMessage(lang.get(sender, "playerlist.navigation", navPlaceholders));
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            List<String> playerDisplays = new ArrayList<>();

            boolean useLuckPerms = plugin.getConfigManager().isPlayerListLuckPermsEnabled() && plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null;
            LuckPerms luckPerms = null;
            if (useLuckPerms) {
                RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) luckPerms = provider.getProvider();
            }

            for (Player player : onlinePlayers) {
                String display;
                if (useLuckPerms && luckPerms != null) {
                    User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                    if (user != null) {
                        String prefix = user.getCachedData().getMetaData().getPrefix();
                        String suffix = user.getCachedData().getMetaData().getSuffix();
                        StringBuilder sb = new StringBuilder();
                        if (prefix != null) sb.append(LegacyColorConverter.toMiniMessage(prefix));
                        sb.append(player.getName());
                        if (suffix != null) sb.append(LegacyColorConverter.toMiniMessage(suffix));
                        display = sb.toString();
                    } else {
                        display = player.getName();
                    }
                } else {
                    display = player.getName();
                }
                playerDisplays.add(display);
            }

            playerDisplays.sort(String.CASE_INSENSITIVE_ORDER);
            PaginatedList<String> paginated = new PaginatedList<>(playerDisplays, 10);
            int totalPages = paginated.getTotalPages();

            return java.util.stream.IntStream.rangeClosed(1, totalPages).mapToObj(String::valueOf).filter(n -> n.startsWith(args[0])).toList();
        }
        return Collections.emptyList();
    }
}