package net.godlycow.org.essc.command.warp;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.util.PaginatedList;
import net.godlycow.org.essc.modules.warp.Warp;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class WarpsCommand extends Command {

    private static final int PER_PAGE = 20;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public WarpsCommand(EssentialsC plugin) {
        super(plugin, "warps", "essentialsc.warps", true, 0, "command.usage.warps");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!plugin.getConfigManager().isWarpEnabled()) {
            player.sendMessage(lang.get(player, "warp.disabled"));
            plugin.debug("Warps command blocked: warp system disabled in config");
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {}
        }

        List<Warp> visibleWarps = plugin.getWarpManager().getVisibleWarps().stream()
                .filter(w -> w.getPermission() == null || player.hasPermission(w.getPermission()))
                .collect(Collectors.toList());

        if (visibleWarps.isEmpty()) {
            player.sendMessage(lang.get(player, "warp.no_warps"));
            return true;
        }

        if (plugin.getConfigManager().isWarpGroupByCategory()) {
            sendCategorizedList(player, visibleWarps, page);
        } else {
            sendFlatList(player, visibleWarps, page);
        }

        return true;
    }

    private void sendFlatList(Player player, List<Warp> warps, int page) {
        PaginatedList<Warp> paged = new PaginatedList<>(warps, PER_PAGE);
        page = paged.clamp(page);

        player.sendMessage(lang.get(player, "warp.list_header"));

        StringBuilder sb = new StringBuilder();
        List<Warp> pageWarps = paged.getPage(page);

        for (int i = 0; i < pageWarps.size(); i++) {
            Warp w = pageWarps.get(i);

            sb.append("<click:run_command:/warp ").append(w.getName()).append(">");
            sb.append("<hover:show_text:'");
            sb.append("World: ").append(w.getLocation().getWorld().getName()).append("\\n");
            if (!w.getDescription().isEmpty()) {
                sb.append(w.getDescription()).append("\\n");
            }
            if (w.getCost() > 0 && plugin.getConfigManager().isEconomyEnabled()) {
                sb.append("Cost: ").append(String.format("%.2f", w.getCost()))
                        .append(" ").append(plugin.getConfigManager().getCurrencyPlural());
            }
            sb.append("'>");
            sb.append("<color:#06FFA5>").append(w.getName()).append("</color>");
            sb.append("</hover>");
            sb.append("</click>");

            if (i < pageWarps.size() - 1) {
                sb.append("<color:#888888>, </color>");
            }

            if ((i + 1) % 5 == 0 && i < pageWarps.size() - 1) {
                sb.append("\n");
            }
        }

        player.sendMessage(mm.deserialize(sb.toString()));

        sendPageFooter(player, page, paged);
    }

    private void sendCategorizedList(Player player, List<Warp> warps, int page) {
        List<Warp> sorted = warps.stream()
                .sorted(Comparator.comparing(Warp::getCategory).thenComparing(Warp::getName))
                .collect(Collectors.toList());

        PaginatedList<Warp> paged = new PaginatedList<>(sorted, PER_PAGE);
        page = paged.clamp(page);

        player.sendMessage(lang.get(player, "warp.list_header"));

        Map<String, List<Warp>> byCategory = paged.getPage(page).stream()
                .collect(Collectors.groupingBy(Warp::getCategory, LinkedHashMap::new, Collectors.toList()));

        byCategory.forEach((category, categoryWarps) -> {
            Map<String, String> catPlaceholder = new HashMap<>();
            catPlaceholder.put("category", category);
            player.sendMessage(lang.get(player, "warp.category_header", catPlaceholder));

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < categoryWarps.size(); i++) {
                Warp w = categoryWarps.get(i);
                sb.append("<click:run_command:/warp ").append(w.getName()).append(">");
                sb.append("<hover:show_text:'").append(w.getLocation().getWorld().getName()).append("'>");
                sb.append("<color:#FFE66D>").append(w.getName()).append("</color>");
                sb.append("</hover>");
                sb.append("</click>");

                if (i < categoryWarps.size() - 1) {
                    sb.append("<color:#888888>, </color>");
                }
            }
            player.sendMessage(mm.deserialize(sb.toString()));
        });

        sendPageFooter(player, page, paged);
    }

    private void sendPageFooter(Player player, int page, PaginatedList<Warp> paged) {
        if (paged.getTotalPages() <= 1) return;

        StringBuilder footer = new StringBuilder("<gray>Page <white>")
                .append(page).append("</white>/").append(paged.getTotalPages());

        if (paged.hasPreviousPage(page)) {
            footer.append("  <click:run_command:/warps ").append(page - 1)
                    .append("><gray>[</gray><white>←</white><gray>]</gray></click>");
        }
        if (paged.hasNextPage(page)) {
            footer.append("  <click:run_command:/warps ").append(page + 1)
                    .append("><gray>[</gray><white>→</white><gray>]</gray></click>");
        }

        player.sendMessage(mm.deserialize(footer.toString()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().isWarpEnabled()) return Collections.emptyList();
        if (args.length == 1) {
            PaginatedList<Warp> paged = new PaginatedList<>(
                    plugin.getWarpManager().getVisibleWarps(), PER_PAGE);
            return List.of("1", "2", "3").stream()
                    .filter(n -> Integer.parseInt(n) <= paged.getTotalPages())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}