package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import net.godlycow.org.essc.modules.punishment.PunishmentManager.BanEntry;
import net.godlycow.org.essc.modules.punishment.PunishmentManager.IpBanEntry;
import net.godlycow.org.essc.util.DurationParser;
import net.godlycow.org.essc.util.PaginatedList;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.stream.Collectors;

public class BanListCommand extends Command {

    private final PunishmentManager punishmentManager;
    private static final int PER_PAGE = 10;

    public BanListCommand(EssentialsC plugin, PunishmentManager punishmentManager) {
        super(plugin, "banlist", "essentialsc.banlist", false, 0, "command.usage.banlist");
        this.punishmentManager = punishmentManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String subCommand = "all";
        int page = 1;

        for (String arg : args) {
            if (arg.matches("\\d+")) {
                try {
                    page = Math.max(1, Integer.parseInt(arg));
                } catch (NumberFormatException ignored) {}
            } else {
                subCommand = arg.toLowerCase();
            }
        }

        switch (subCommand) {
            case "players", "player", "p" -> showPlayerBans(sender, page);
            case "ips",     "ip",     "i" -> showIpBans(sender, page);
            case "history", "h"           -> showRecentBans(sender, page);
            default                       -> showAllBans(sender, page);
        }

        return true;
    }


    private void showPlayerBans(CommandSender sender, int page) {
        List<BanEntry> bans = punishmentManager.getActiveBans();
        PaginatedList<BanEntry> paged = new PaginatedList<>(bans, PER_PAGE);

        if (paged.isEmpty()) {
            sender.sendMessage(lang.get(sender, "banlist.no_player_bans"));
            return;
        }

        page = paged.clamp(page);
        showHeader(sender, "player", bans.size(), page, paged.getTotalPages());

        int index = paged.startIndex(page);
        for (BanEntry entry : paged.getPage(page)) {
            sender.sendMessage(playerBanEntry(sender, entry, index++));
        }

        showFooter(sender, page, paged.getTotalPages());
    }

    private void showIpBans(CommandSender sender, int page) {
        List<IpBanEntry> bans = punishmentManager.getActiveIpBans();
        PaginatedList<IpBanEntry> paged = new PaginatedList<>(bans, PER_PAGE);

        if (paged.isEmpty()) {
            sender.sendMessage(lang.get(sender, "banlist.no_ip_bans"));
            return;
        }

        page = paged.clamp(page);
        showHeader(sender, "ip", bans.size(), page, paged.getTotalPages());

        int index = paged.startIndex(page);
        for (IpBanEntry entry : paged.getPage(page)) {
            sender.sendMessage(ipBanEntry(sender, entry, index++));
        }

        showFooter(sender, page, paged.getTotalPages());
    }

    private void showAllBans(CommandSender sender, int page) {
        List<Object> all = new ArrayList<>();
        all.addAll(punishmentManager.getActiveBans());
        all.addAll(punishmentManager.getActiveIpBans());

        if (all.isEmpty()) {
            sender.sendMessage(lang.get(sender, "banlist.no_bans"));
            return;
        }

        all.sort((a, b) -> {
            long ta = (a instanceof BanEntry be)     ? be.time() : ((IpBanEntry) a).time();
            long tb = (b instanceof BanEntry be)     ? be.time() : ((IpBanEntry) b).time();
            return Long.compare(tb, ta);
        });

        PaginatedList<Object> paged = new PaginatedList<>(all, PER_PAGE);
        page = paged.clamp(page);

        sender.sendMessage(lang.get(sender, "banlist.all.header", Map.of(
                "page",    String.valueOf(page),
                "total",   String.valueOf(paged.getTotalPages()),
                "count",   String.valueOf(all.size()),
                "players", String.valueOf(punishmentManager.getActiveBans().size()),
                "ips",     String.valueOf(punishmentManager.getActiveIpBans().size())
        )));

        int index = paged.startIndex(page);
        for (Object entry : paged.getPage(page)) {
            if (entry instanceof BanEntry be)        sender.sendMessage(playerBanEntry(sender, be, index));
            else if (entry instanceof IpBanEntry ie) sender.sendMessage(ipBanEntry(sender, ie, index));
            index++;
        }

        showFooter(sender, page, paged.getTotalPages());
        sender.sendMessage(lang.get(sender, "banlist.filter_hint"));
    }

    private void showRecentBans(CommandSender sender, int page) {
        List<Object> all = new ArrayList<>();
        all.addAll(punishmentManager.getAllBans());
        all.addAll(punishmentManager.getAllIpBans());
        all.sort((a, b) -> {
            long ta = (a instanceof BanEntry be)     ? be.time() : ((IpBanEntry) a).time();
            long tb = (b instanceof BanEntry be)     ? be.time() : ((IpBanEntry) b).time();
            return Long.compare(tb, ta);
        });

        PaginatedList<Object> paged = new PaginatedList<>(all, PER_PAGE);
        page = paged.clamp(page);

        sender.sendMessage(lang.get(sender, "banlist.history.header"));

        int index = paged.startIndex(page);
        for (Object entry : paged.getPage(page)) {
            if (entry instanceof BanEntry be)        sender.sendMessage(playerBanEntry(sender, be, index));
            else if (entry instanceof IpBanEntry ie) sender.sendMessage(ipBanEntry(sender, ie, index));
            index++;
        }

        showFooter(sender, page, paged.getTotalPages());
    }

    private void showHeader(CommandSender sender, String type, int count, int page, int totalPages) {
        sender.sendMessage(lang.get(sender, "banlist." + type + ".header", Map.of(
                "type",  type,
                "count", String.valueOf(count),
                "page",  String.valueOf(page),
                "total", String.valueOf(totalPages)
        )));
    }

    private void showFooter(CommandSender sender, int page, int totalPages) {
        if (totalPages <= 1) return;
        sender.sendMessage(lang.get(sender, "banlist.footer", Map.of(
                "current", String.valueOf(page),
                "total",   String.valueOf(totalPages),
                "prev",    String.valueOf(page - 1),
                "next",    String.valueOf(page + 1)
        )));
    }

    private Component playerBanEntry(CommandSender sender, BanEntry entry, int index) {
        boolean isTemp = entry.expires() > 0;
        return lang.get(sender, isTemp ? "banlist.entry.player.temp" : "banlist.entry.player.perm", Map.of(
                "index",   String.valueOf(index),
                "player",  entry.name(),
                "reason",  entry.reason(),
                "banner",  entry.banner(),
                "time",    DurationParser.formatAgo(entry.time()),
                "expires", isTemp ? DurationParser.formatRemaining(entry.expires()) : "Never"
        ));
    }

    private Component ipBanEntry(CommandSender sender, IpBanEntry entry, int index) {
        boolean isTemp = entry.expires() > 0;
        return lang.get(sender, isTemp ? "banlist.entry.ip.temp" : "banlist.entry.ip.perm", Map.of(
                "index",   String.valueOf(index),
                "ip",      entry.ip(),
                "reason",  entry.reason(),
                "banner",  entry.banner(),
                "time",    DurationParser.formatAgo(entry.time()),
                "expires", isTemp ? DurationParser.formatRemaining(entry.expires()) : "Never"
        ));
    }


    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("players", "ips", "all", "history").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].matches("players|ips|all|history")) {
            return List.of("1", "2", "3");
        }
        return Collections.emptyList();
    }
}