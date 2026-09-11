package net.godlycow.org.essc.command.economy;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.util.PaginatedList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class BaltopCommand extends Command {

    private static final int PER_PAGE = 10;

    public BaltopCommand(EssentialsC plugin) {
        super(plugin, "baltop", "essentialsc.baltop", false, 0, "command.usage.baltop");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {}
        }

        final int finalPage = page;
        sender.sendMessage(lang.get(sender, "baltop.loading"));

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            try {
                Map<UUID, BigDecimal> top = plugin.getEconomyManager()
                        .getTopBalances(PER_PAGE * finalPage).get();

                plugin.getServer().getGlobalRegionScheduler().run(plugin, task1 ->
                        displayTop(sender, new ArrayList<>(top.entrySet()), finalPage));

            } catch (InterruptedException | ExecutionException e) {
                sender.sendMessage(lang.get(sender, "error.internal"));
                plugin.debug("Failed to load baltop: " + e.getMessage());
            }
        });

        return true;
    }

    private boolean isExempt(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        return online != null && online.hasPermission("essentialsc.baltop.exempt");
    }

    private void displayTop(CommandSender sender, List<Map.Entry<UUID, BigDecimal>> entries, int page) {
        entries.removeIf(e -> isExempt(e.getKey()));

        PaginatedList<Map.Entry<UUID, BigDecimal>> paged = new PaginatedList<>(entries, PER_PAGE);

        if (paged.isEmpty()) {
            sender.sendMessage(lang.get(sender, "baltop.empty"));
            return;
        }

        if (!paged.isValidPage(page)) {
            sender.sendMessage(lang.get(sender, "baltop.empty"));
            return;
        }

        sender.sendMessage(lang.get(sender, "baltop.header", Map.of("page", String.valueOf(page))));

        int rank = paged.startIndex(page);
        for (Map.Entry<UUID, BigDecimal> entry : paged.getPage(page)) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Unknown";

            sender.sendMessage(lang.get(sender, "baltop.entry", Map.of(
                    "rank",    String.valueOf(rank++),
                    "player",  name,
                    "balance", plugin.getEconomyManager().format(entry.getValue())
            )));
        }

        if (paged.hasNextPage(page)) {
            sender.sendMessage(lang.get(sender, "baltop.next", Map.of("page", String.valueOf(page + 1))));
        }

        sender.sendMessage(lang.get(sender, "baltop.footer"));
    }
}