package net.godlycow.org.essc.modules.scoreboard;

import net.godlycow.org.essc.server.software.ServerSoftware;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerScoreboard {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String[] INVISIBLE_ENTRIES = generateInvisibleEntries(32);

    private final UUID playerId;
    private org.bukkit.scoreboard.Scoreboard scoreboard;
    private Objective objective;
    private final Team[] teams;
    private final String[] lastValues;
    private final int lineCount;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private String lastTitle = "";

    public PlayerScoreboard(Player player, ScoreboardConfig config) {
        this.playerId = player.getUniqueId();
        this.lineCount = Math.min(config.getLineCount(), 32);

        if (ServerSoftware.isFolia()) {
            this.scoreboard = null;
            this.teams = new Team[0];
            this.lastValues = new String[0];
            this.objective = null;
            destroyed.set(true);
            return;
        }

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.teams = new Team[lineCount];
        this.lastValues = new String[lineCount];

        this.objective = scoreboard.registerNewObjective("essc_sb_" + playerId.toString().substring(0, 8), Criteria.DUMMY,
                Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (int i = 0; i < lineCount; i++) {
            String entry = INVISIBLE_ENTRIES[i];
            Team team = scoreboard.registerNewTeam("line_" + i + "_" + playerId.toString().substring(0, 4));
            team.addEntry(entry);
            teams[i] = team;
            objective.getScore(entry).setScore(lineCount - i);
        }
    }

    public void show(Player player) {
        if (destroyed.get()) return;
        if (!player.getUniqueId().equals(playerId)) return;

        try {
            player.setScoreboard(scoreboard);
        } catch (Exception e) {
        }
    }

    public void updateProcessed(Player player, String processedTitle, List<String> processedLines) {
        if (destroyed.get() || !player.isOnline()) return;
        if (processedLines.size() < lineCount) return;

        try {
            if (!processedTitle.equals(lastTitle)) {
                lastTitle = processedTitle;
                objective.displayName(MINI_MESSAGE.deserialize(processedTitle));
            }

            for (int i = 0; i < lineCount && i < processedLines.size(); i++) {
                String processed = processedLines.get(i);

                if (!processed.equals(lastValues[i])) {
                    lastValues[i] = processed;
                    Component component = MINI_MESSAGE.deserialize(processed);
                    teams[i].prefix(component);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Error updating scoreboard for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void hide(Player player) {
        if (destroyed.get()) return;

        try {
            if (player.isOnline() && player.getScoreboard() == scoreboard) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        } catch (Exception e) {
        }
    }

    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            try {
                for (Team team : teams) {
                    if (team != null) {
                        try {
                            team.unregister();
                        } catch (Exception ignored) {}
                    }
                }

                if (objective != null) {
                    try {
                        objective.unregister();
                    } catch (Exception ignored) {}
                }

                scoreboard = null;
            } catch (Exception e) {
                Bukkit.getLogger().warning("Error destroying scoreboard: " + e.getMessage());
            }
        }
    }

    public boolean isActive() {
        return !destroyed.get() && scoreboard != null;
    }

    private static String[] generateInvisibleEntries(int size) {
        String[] entries = new String[size];
        ChatColor[] colors = ChatColor.values();

        for (int i = 0; i < size; i++) {
            StringBuilder sb = new StringBuilder();
            int num = i;

            while (num >= 0) {
                sb.append(colors[num % 16]);
                num = num / 16 - 1;
            }

            entries[i] = sb.toString();
        }
        return entries;
    }
}