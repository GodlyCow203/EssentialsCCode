package net.godlycow.org.essc.language;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.godlycow.org.essc.EssentialsC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LanguageManager {
    private final EssentialsC plugin;
    private final MiniMessage miniMessage;
    private final Gson gson = new Gson();

    private final Map<String, Map<String, String>> cache = new HashMap<>();
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private String defaultLang;

    public LanguageManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
    }

    public void load(String defaultLanguage) {
        this.defaultLang = defaultLanguage;
        cache.clear();

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        String[] languages = {
                "th_TH","sv_SE","fil_PH","vi_VN","id_ID","uk_UA","pl_PL","nl_NL",
                "it_IT","te_IN","ta_IN","gu_IN","bn_BD","mr_IN","hi_IN","de_DE",
                "ur_PK","tr_TR","ar_SA","ko_KR","ru_RU","ja_JP","fr_FR",
                "zh_CN","pt_BR","es_ES","en_US", "LB_lb"
        };


        for (String lang : languages) {
            File file = new File(langFolder, lang + ".json");
            if (!file.exists()) {
                plugin.saveResource("lang/" + lang + ".json", false);
            }
            migrateLangFile(lang);
        }
        loadIntoCache(defaultLanguage);
        for (String lang : languages) {
            if (!cache.containsKey(lang)) {
                loadIntoCache(lang);
            }
        }

        plugin.debug("Loaded default language: " + defaultLanguage);
    }


    private void loadIntoCache(String code) {
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".json");
        if (!file.exists()) {
            if (code.equals(defaultLang)) {
                plugin.getLogger().severe("Default language file missing: " + code + ".json");
            }
            plugin.debug("Language file not found: " + code + ".json");
            return;
        }

        Map<String, String> messages = new HashMap<>();
        try (var reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json != null) {
                json.entrySet().forEach(e -> messages.put(e.getKey(), e.getValue().getAsString()));
            }
            cache.put(code, messages);
            plugin.debug("Cached language: " + code + " (" + messages.size() + " keys)");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load language " + code + ": " + e.getMessage());
            plugin.debug("Exception loading " + code + ": " + e.getMessage());
        }
    }

    public @NotNull Component get(CommandSender sender, String key, Map<String, String> placeholders) {
        String raw = resolve(sender, key, placeholders);

        if (sender != null && !(sender instanceof Player) && plugin.getConfigManager().isChatStripColorsFromConsole()) {
            Component component = miniMessage.deserialize(raw);
            String plain = PlainTextComponentSerializer.plainText().serialize(component);
            return Component.text(plain);
        }

        return miniMessage.deserialize(raw);
    }

    public Component get(CommandSender sender, String key) {
        return get(sender, key, null);
    }

    /**
     * Resolves a key to its raw MiniMessage string (placeholders and {@code <prefix>} substituted) without
     * parsing it into a {@link Component}. Useful when a resolved value must be embedded as a placeholder
     * inside another message before the combined string is parsed.
     */
    public String getRaw(CommandSender sender, String key, Map<String, String> placeholders) {
        return resolve(sender, key, placeholders);
    }

    public String getRaw(CommandSender sender, String key) {
        return resolve(sender, key, null);
    }

    private String resolve(CommandSender sender, String key, Map<String, String> placeholders) {
        String locale = defaultLang;

        if (sender instanceof Player player) {
            String playerLang = playerLanguages.get(player.getUniqueId());
            if (playerLang != null) {
                locale = playerLang;
            } else {
                locale = player.locale().toString();
            }
        }

        if (!cache.containsKey(locale)) {
            loadIntoCache(locale);
        }

        String fallbackLang = plugin.getConfigManager().getFallbackLanguage();

        Map<String, String> messages = cache.get(locale);
        if (messages == null) {
            plugin.getLogger().warning("[EssentialsC] Language '" + locale + "' not loaded, falling back to '" + fallbackLang + "'");
            if (!cache.containsKey(fallbackLang)) loadIntoCache(fallbackLang);
            messages = cache.get(fallbackLang);
        }
        if (messages == null) {
            plugin.getLogger().warning("[EssentialsC] Fallback language '" + fallbackLang + "' not loaded, falling back to default '" + defaultLang + "'");
            messages = cache.get(defaultLang);
        }
        if (messages == null) {
            plugin.getLogger().severe("[EssentialsC] No language files loaded at all — cannot resolve key '" + key + "'");
            return "<red>Missing lang files</red>";
        }

        String raw = messages.get(key);

        if (raw == null && !locale.equals(fallbackLang)) {
            Map<String, String> fallbackMessages = cache.get(fallbackLang);
            if (fallbackMessages != null) {
                raw = fallbackMessages.get(key);
                if (raw != null) {
                    plugin.getLogger().warning("[EssentialsC] Missing key '" + key + "' in '" + locale + "', using fallback '" + fallbackLang + "'");
                }
            }
        }

        if (raw == null && !locale.equals(defaultLang) && !fallbackLang.equals(defaultLang)) {
            Map<String, String> defaultMessages = cache.get(defaultLang);
            if (defaultMessages != null) {
                raw = defaultMessages.get(key);
                if (raw != null) {
                    plugin.getLogger().warning("[EssentialsC] Missing key '" + key + "' in '" + locale + "' and '" + fallbackLang + "', using default '" + defaultLang + "'");
                }
            }
        }

        if (raw == null) {
            plugin.getLogger().warning("[EssentialsC] Missing key '" + key + "' in all languages (locale='" + locale + "', fallback='" + fallbackLang + "', default='" + defaultLang + "')");
            raw = messages.getOrDefault("error.missing_key", "<red>Missing key: <key></red>");
            raw = raw.replace("<key>", key);
        }

        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                raw = raw.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }

        String prefix = messages.get("prefix");
        if (prefix == null && cache.containsKey(fallbackLang)) prefix = cache.get(fallbackLang).get("prefix");
        if (prefix == null && cache.containsKey(defaultLang)) prefix = cache.get(defaultLang).get("prefix");
        if (prefix != null) raw = raw.replace("<prefix>", prefix);

        return raw;
    }

    public void setPlayerLanguage(UUID playerUuid, String languageCode) {
        playerLanguages.put(playerUuid, languageCode);
        if (!cache.containsKey(languageCode)) {
            loadIntoCache(languageCode);
        }
    }

    public void removePlayerLanguage(UUID playerUuid) {
        playerLanguages.remove(playerUuid);
    }

    public String getPlayerLanguage(UUID playerUuid) {
        return playerLanguages.get(playerUuid);
    }

    public boolean hasPlayerLanguage(UUID playerUuid) {
        return playerLanguages.containsKey(playerUuid);
    }

    public Map<UUID, String> getPlayerLanguages() {
        return new HashMap<>(playerLanguages);
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public void reload() {
        plugin.debug("Reloading language manager...");
        load(defaultLang);
    }

    private static final com.google.gson.Gson PRETTY_GSON = new com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private void migrateLangFile(String code) {
        File file = new File(plugin.getDataFolder(), "lang/" + code + ".json");
        if (!file.exists()) return;

        var resource = plugin.getResource("lang/" + code + ".json");
        if (resource == null) return;

        try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            JsonObject defaults = gson.fromJson(reader, JsonObject.class);
            if (defaults == null) return;

            JsonObject existing;
            try (var fr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                existing = gson.fromJson(fr, JsonObject.class);
                if (existing == null) existing = new JsonObject();
            }

            boolean dirty = false;
            for (var entry : defaults.entrySet()) {
                if (!existing.has(entry.getKey())) {
                    existing.add(entry.getKey(), entry.getValue());
                    dirty = true;
                    plugin.debug("Migrated missing lang key '" + entry.getKey() + "' in " + code);
                }
            }

            if (dirty) {
                try (var writer = new java.io.FileWriter(file, StandardCharsets.UTF_8)) {
                    PRETTY_GSON.toJson(existing, writer);
                }
                plugin.debug("[EssentialsC] Migrated missing keys in lang/" + code + ".json");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to migrate lang file " + code + ": " + e.getMessage());
        }
    }
}