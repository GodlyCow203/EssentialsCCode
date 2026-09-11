package net.godlycow.org.essc.modules.scoreboard;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.LegacyColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreboardConfig {

    private final boolean enabled;
    private final int updateInterval;
    private final boolean persistent;
    private final Component title;
    private final String titleRaw;
    private final List<String> lines;
    private final List<String[]> lineTemplates;
    private final EssentialsC plugin;

    public ScoreboardConfig(EssentialsC plugin) {

        this.plugin = plugin;
        //use getConfigManager
        this.enabled = plugin.getConfigManager().isScoreboardEnabled();
        this.updateInterval = Math.max(1, plugin.getConfigManager().getScoreboardUpdateInterval());
        this.persistent = plugin.getConfigManager().isScoreboardPersistenceEnabled();
        this.titleRaw = plugin.getConfigManager().getRawScoreboardTitle();

        this.title = MiniMessage.miniMessage().deserialize(titleRaw);
        List<String> rawLines = plugin.getConfigManager().getScoreboardstringList();

        if (rawLines.isEmpty()) {
            rawLines = List.of("", "", "", "");
        }

        List<String> processedLines = new ArrayList<>(rawLines.size());
        for (String line : rawLines) {
            processedLines.add(LegacyColorConverter.toMiniMessage(line));
        }
        this.lines = Collections.unmodifiableList(processedLines);

        this.lineTemplates = new ArrayList<>(processedLines.size());
        for (String line : processedLines) {
            lineTemplates.add(splitTemplate(line));
        }
    }

    private String[] splitTemplate(String line) {
        if (line == null) return new String[]{""};
        return line.split("(?=%)", -1);
    }

    public boolean isEnabled() {
        return enabled;
    }
    public int getUpdateInterval() {
        return updateInterval;
    }
    public Component getTitle() {
        return title;
    }
    public String getTitleRaw() {
        return titleRaw;
    }
    public List<String> getLines() {
        return lines;
    }
    public int getLineCount() {
        return lines.size();
    }
}