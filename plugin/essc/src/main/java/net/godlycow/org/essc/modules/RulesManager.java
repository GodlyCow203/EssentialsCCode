package net.godlycow.org.essc.modules;

import net.godlycow.org.essc.EssentialsC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class RulesManager {
    private final EssentialsC plugin;
    private final MiniMessage miniMessage;
    private final List<Component> rules = new ArrayList<>();
    private final File rulesFile;

    public RulesManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.rulesFile = new File(plugin.getDataFolder(), "rules.txt");
    }

    public void load() {
        rules.clear();

        try {
            if (!rulesFile.exists()) {
                if (rulesFile.createNewFile()) {
                    plugin.debug("Created rules.txt");
                    createDefaultRules();
                }
            }

            if (rulesFile.length() == 0) {
                createDefaultRules();
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(Files.newInputStream(rulesFile.toPath()), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    Component parsed = miniMessage.deserialize(line);
                    rules.add(parsed);
                }

                plugin.debug("Loaded " + rules.size() + " rules from rules.txt");

            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load rules.txt: " + e.getMessage());
        }
    }

    private void createDefaultRules() {
        List<String> defaultRules = List.of(
                "<#FFD700><bold>=== Server Rules ===</bold></#FFD700>",
                "",
                "<#FF5555>1.</#FF5555> <#FFFF55>Be respectful to all players</#FFFF55>",
                "<#FF5555>2.</#FF5555> <#55FFFF>No griefing or stealing</#55FFFF>",
                "<#FF5555>3.</#FF5555> <#FF55FF>No cheating or hacked clients</#FF55FF>",
                "<#FF5555>4.</#FF5555> <#55FF55>No spamming or advertising</#55FF55>",
                "<#FF5555>5.</#FF5555> <#FFAA00>Have fun!</#FFAA00>",
                "",
                "<#FFD700><bold>======================</bold></#FFD700>"
        );

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(rulesFile.toPath()), StandardCharsets.UTF_8))) {

            for (String line : defaultRules) {
                writer.write(line);
                writer.newLine();
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default rules.txt: " + e.getMessage());
        }
    }

    public List<Component> getRules() {
        return new ArrayList<>(rules);
    }

    public void reload() {
        plugin.debug("Reloading rules...");
        load();
    }
}