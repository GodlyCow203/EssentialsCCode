package net.godlycow.org.essc.plugin.config;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.List;

public class EssConfig {
    private final EssentialsC plugin;
    private FileConfiguration config;
    private final ConfigMigrator migrator;

    public EssConfig(EssentialsC plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.migrator = new ConfigMigrator(plugin);
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        migrate();
        plugin.debug("Configuration file reloaded");
    }

    public String getDefaultLanguage() {
        return config.getString("default-language", "en_US");
    }

    public String getHelpColorLabel() {
        return config.getString("help.colors.label", "<color:#FFF200>");
    }

    public String getHelpColorHeader() {
        return config.getString("help.colors.header", "<color:#FFF200>");
    }

    public String getHelpColorBody() {
        return config.getString("help.colors.body", "<gray>");
    }

    public String getHelpColorDivider() {
        return config.getString("help.colors.divider", "<dark_gray>");
    }

    public String getHelpColorArgRequired() {
        return config.getString("help.colors.arg-required", "<white>");
    }

    public String getHelpColorArgOptional() {
        return config.getString("help.colors.arg-optional", "<gray>");
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }

    public void setDebug(boolean debug) {
        config.set("debug", debug);
        plugin.saveConfig();
    }

    public void migrate() {
        migrator.migrateConfig();
        config = plugin.getConfig();
    }

    public boolean isEconomyEnabled() {
        return config.getBoolean("economy.enabled", true);
    }

    public String getCurrencySingular() {
        return config.getString("economy.currency.singular", "Dollar");
    }

    public String getCurrencyPlural() {
        return config.getString("economy.currency.plural", "Dollars");
    }

    public BigDecimal getStartingBalance() {
        return new BigDecimal(config.getString("economy.starting-balance", "100.00"));
    }

    public BigDecimal getMinTransaction() {
        return new BigDecimal(config.getString("economy.minimum-transaction", "0.01"));
    }

    public BigDecimal getMaxBalance() {
        String val = config.getString("economy.max-balance", "-1");
        return val.equals("-1") ? null : new BigDecimal(val);
    }

    public String getEconomyFormat() {
        return config.getString("economy.format", "#,##0.00");
    }

    public long getTPACooldown() {
        return config.getLong("tpa.cooldown", 60);
    }
    public long getTPAWarmup() {
        return config.getLong("tpa.warmup", 3);
    }
    public long getTPATimeout() {
        return config.getLong("tpa.timeout", 60);
    }
    public int getTPAMaxPending() {
        return config.getInt("tpa.max-pending", 5);
    }
    public int getTPAMaxOutgoing() {
        return config.getInt("tpa.max-outgoing", 1);
    }
    public boolean isTPADenyMovement() {
        return config.getBoolean("tpa.deny-movement", true);
    }
    public boolean isTPAParticles() {
        return config.getBoolean("tpa.particles", true);
    }
    public boolean isTPASounds() {
        return config.getBoolean("tpa.sounds", true);
    }
    public List<String> getTPABlockedWorlds() {
        return config.getStringList("tpa.blocked-worlds");
    }

    public int getMaxHomes() {
        return config.getInt("home.max-homes", 3);
    }

    public long getHomeCooldown() {
        return config.getLong("home.cooldown", 5);
    }

    public long getHomeWarmup() {
        return config.getLong("home.warmup", 3);
    }

    public boolean isHomeCancelOnMovement() {
        return config.getBoolean("home.cancel-on-movement", true);
    }

    public boolean isHomeParticles() {
        return config.getBoolean("home.particles", true);
    }

    public boolean isHomeSounds() {
        return config.getBoolean("home.sounds", true);
    }

    public List<String> getHomeBlockedWorlds() {
        return config.getStringList("home.blocked-worlds");
    }

    public String getDefaultHomeName() {
        return config.getString("home.default-name", "home");
    }

    public String getDefaultTeleportHomeName() {
        return config.getString("home.default-teleport-name", "");
    }

    public String getKitMode() {
        return config.getString("kits.mode", "command").toLowerCase();
    }

    public boolean isKitGuiMode() {
        return getKitMode().equals("gui");
    }

    public boolean isKitGuiSoundsEnabled() {
        return config.getBoolean("kits.sounds", true);
    }

    public boolean isBedHomeCountsInLimit() {
        return config.getBoolean("home.bed-counts-in-limit", true);
    }

    public long getSpawnCooldown() {
        return config.getLong("spawn.cooldown", 5);
    }

    public long getSpawnWarmup() {
        return config.getLong("spawn.warmup", 3);
    }

    public boolean isSpawnFirstJoin() {
        return config.getBoolean("spawn.teleport-on-first-join", true);
    }

    public boolean isSpawnParticles() {
        return config.getBoolean("spawn.particles", true);
    }

    public boolean isSpawnSounds() {
        return config.getBoolean("spawn.sounds", true);
    }

    public boolean isSpawnCancelOnMovement() {
        return config.getBoolean("spawn.cancel-on-movement", true);
    }

    public boolean isJoinLeaveEnabled() {
        return config.getBoolean("join-leave-messages.enabled", true);
    }

    public String getJoinMessage() {
        return config.getString("join-leave-messages.join", "<yellow><player> joined the game");  }

    public String getLeaveMessage() {
        return config.getString("join-leave-messages.leave", "<yellow><player> left the game");
    }

    public long getBackWarmup() {
        return config.getLong("back.warmup", 0);
    }

    public long getBackCooldown() {
        return config.getLong("back.cooldown", 0);
    }

    public boolean isBackParticles() {
        return config.getBoolean("back.particles", true);
    }

    public boolean isBackSounds() {
        return config.getBoolean("back.sounds", true);
    }

    public boolean isBackCancelOnMovement() {
        return config.getBoolean("back.cancel-on-movement", true);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isScoreboardEnabled() {
        return config.getBoolean("scoreboard.enabled", false);
    }

    public boolean isRenameBlacklistEnabled() {
        return config.getBoolean("rename.blacklist-enabled", true);
    }

    public List<String> getRenameBlacklistWords() {
        return config.getStringList("rename.blacklist-words");
    }

    public int getRenameMaxLength() {
        return config.getInt("rename.max-length", 50);
    }

    public int getRenameMinLength() {
        return config.getInt("rename.min-length", 1);
    }

    public boolean isRenameNormalizeEnabled() {
        return config.getBoolean("rename.normalize-enabled", true);
    }

    public boolean isRenameStripColorsEnabled() {
        return config.getBoolean("rename.strip-colors-for-check", true);
    }

    public boolean isVanishHideFromTab() {
        return config.getBoolean("vanish.hide-from-tab", true);
    }

    public boolean isVanishNightVision() {
        return config.getBoolean("vanish.give-night-vision", true);
    }

    public boolean isVanishPreventMobTarget() {
        return config.getBoolean("vanish.prevent-mob-target", true);
    }

    public boolean isVanishDisableCollisions() {
        return config.getBoolean("vanish.disable-collisions", true);
    }

    public boolean isVanishHideFromServerList() {
        return config.getBoolean("vanish.hide-from-server-list", true);
    }

    public boolean isNickEnabled() {
        return config.getBoolean("nickname.enabled", true);
    }

    public int getNickMinLength() {
        return config.getInt("nickname.min-length", 3);
    }

    public int getNickMaxLength() {
        return config.getInt("nickname.max-length", 16);
    }

    public boolean isNickBlacklistEnabled() {
        return config.getBoolean("nickname.blacklist-enabled", true);
    }

    public List<String> getNickBlacklistWords() {
        return config.getStringList("nickname.blacklist-words");
    }

    public boolean isNickNormalizeEnabled() {
        return config.getBoolean("nickname.normalize-enabled", true);
    }

    public boolean isNickUnique() {
        return config.getBoolean("nickname.unique", true);
    }

    public boolean isNickTabEnabled() {
        return config.getBoolean("nickname.update-tab", true);
    }

    public String getNickIndicator() {
        return config.getString("nickname.indicator", "~");
    }

    public boolean isShopEnabled() {
        return config.getBoolean("shop.enabled", true);
    }

    public List<String> getDiscordMessage() {
        return config.getStringList("discord.message");
    }

    public String getShopCurrencySingular() {
        return config.getString("shop.currency.singular", "Dollar");
    }

    public String getShopCurrencyPlural() {
        return config.getString("shop.currency.plural", "Dollars");
    }

    public boolean isShopLogTransactions() {
        return config.getBoolean("shop.log-transactions", true);
    }

    public boolean isHatBlacklistEnabled() {
        return config.getBoolean("hat.blacklist-enabled", true);
    }

    public List<String> getHatBlacklistItems() {
        return config.getStringList("hat.blacklist-items");
    }

    public boolean isHatAllowBlocks() {
        return config.getBoolean("hat.allow-blocks", false);
    }

    public boolean isHatRequireLore() {
        return config.getBoolean("hat.require-lore", false);
    }

    public boolean isHatRequireName() {
        return config.getBoolean("hat.require-custom-name", false);
    }

    public boolean isAHEnabled() {
        return config.getBoolean("auction-house.enabled", true);
    }

    public int getAHMaxAuctions() {
        return config.getInt("auction-house.max-auctions-per-player", 5);
    }

    public long getAHDuration() {
        return config.getLong("auction-house.duration-hours", 48) * 60 * 60 * 1000;
    }

    public BigDecimal getAHMinPrice() {
        return new BigDecimal(config.getString("auction-house.min-price", "1.00"));
    }

    public BigDecimal getAHMaxPrice() {
        String val = config.getString("auction-house.max-price", "-1");
        return val.equals("-1") ? null : new BigDecimal(val);
    }

    public boolean isAHAllowEnchantedBooks() {
        return config.getBoolean("auction-house.allow-enchanted-books", true);
    }

    public List<String> getAHBlacklistedMaterials() {
        return config.getStringList("auction-house.blacklisted-materials");
    }

    public boolean isAHNotifyOnSale() {
        return config.getBoolean("auction-house.notify-on-sale", true);
    }

    public boolean isWarpEnabled() {
        return config.getBoolean("warp.enabled", true);
    }

    public long getWarpCooldown() {
        return config.getLong("warp.cooldown", 5);
    }

    public long getWarpWarmup() {
        return config.getLong("warp.warmup", 3);
    }

    public boolean isWarpCancelOnMovement() {
        return config.getBoolean("warp.cancel-on-movement", true);
    }

    public boolean isWarpParticles() {
        return config.getBoolean("warp.particles", true);
    }

    public boolean isWarpSounds() {
        return config.getBoolean("warp.sounds", true);
    }

    public List<String> getWarpBlockedWorlds() {
        return config.getStringList("warp.blocked-worlds");
    }

    public boolean isWarpGroupByCategory() {
        return config.getBoolean("warp.group-by-category", true);
    }

    public int getWarpMaxNameLength() {
        return config.getInt("warp.max-name-length", 16);
    }

    public boolean isWarpLimitEnabled() {
        return config.getBoolean("warp.limit-per-player.enabled", false);
    }

    public int getWarpMaxPerPlayer() {
        return config.getInt("warp.limit-per-player.max", 5);
    }

    public boolean isAfkEnabled() {
        return config.getBoolean("afk.enabled", true);
    }

    public long getAfkTimeout() {
        return config.getLong("afk.timeout-seconds", 300);
    }

    public long getAfkKickTimeout() {
        return config.getLong("afk.kick-timeout-seconds", 1800);
    }

    public boolean isAfkKickEnabled() {
        return config.getLong("afk.kick-timeout-seconds", 1800) > 0;
    }

    public boolean isAfkTabPlaceholderEnabled() {
        return config.getBoolean("afk.tab-placeholder-enabled", true);
    }

    public String getAfkTabPlaceholder() {
        return config.getString("afk.tab-placeholder", "[AFK] ");
    }

    public boolean isAfkBroadcastEnabled() {
        return config.getBoolean("afk.broadcast-enabled", true);
    }

    public boolean isAfkTitleEnabled() {
        return config.getBoolean("afk.title-enabled", true);
    }

    public String getAfkTitle() {
        return config.getString("afk.title", "<color:#FF6B6B>You are now AFK</color>");
    }

    public String getAfkSubtitle() {
        return config.getString("afk.subtitle", "<color:#FFE66D>Move to return</color>");
    }

    public boolean isAfkPreventDamage() {
        return config.getBoolean("afk.prevent-damage", true);
    }

    public boolean isAfkPreventMobTarget() {
        return config.getBoolean("afk.prevent-mob-target", true);
    }

    public boolean isAfkPreventPickup() {
        return config.getBoolean("afk.prevent-pickup", true);
    }

    public boolean isAfkFreezePlayer() {
        return config.getBoolean("afk.freeze-player", true);
    }

    public List<String> getAfkBlockedCommands() {
        return config.getStringList("afk.blocked-commands");
    }

    public boolean isAfkListShowLocation() {
        return config.getBoolean("afk.list-show-location", true);
    }

    public boolean isAfkListShowWorld() {
        return config.getBoolean("afk.list-show-world", true);
    }

    public String getAfkListSortBy() {
        return config.getString("afk.list-sort-by", "time");
    }

    public boolean isLuckPermsChatEnabled() { return config.getBoolean("luckperms.chat-formatting", true);}

    public boolean isLuckPermsTabEnabled() {
        return config.getBoolean("luckperms.tab-formatting", true);
    }

    public boolean isDiscordSRVEnabled() {
        return config.getBoolean("discordsrv.enabled", true);
    }

    public String getDiscordSRVPunishmentsChannelName() {
        return config.getString("discordsrv.channels.punishments.name", "punishments");
    }

    public boolean isDiscordBanEnabled() {
        return config.getBoolean("discordsrv.events.ban.enabled", true);
    }

    public String getDiscordBanColor() {
        return config.getString("discordsrv.events.ban.color", "#E74C3C");
    }

    public boolean isDiscordKickEnabled() {
        return config.getBoolean("discordsrv.events.kick.enabled", true);
    }

    public String getDiscordKickColor() {
        return config.getString("discordsrv.events.kick.color", "#F39C12");
    }

    public boolean isDiscordSRVShowAvatar() {
        return config.getBoolean("discordsrv.show-avatar", true);
    }

    public String getDiscordSRVAvatarUrl() {
        return config.getString("discordsrv.avatar-url", "https://crafthead.net/helm/{uuid}");
    }

    public boolean isDiscordMuteEnabled() {
        return config.getBoolean("discordsrv.events.mute.enabled", true);
    }

    public String getDiscordMuteColor() {
        return config.getString("discordsrv.events.mute.color", "#9B59B6");
    }

    public boolean isDiscordKitEnabled() {
        return config.getBoolean("discordsrv.events.kit.enabled", true);
    }

    public String getDiscordKitColor() {
        return config.getString("discordsrv.events.kit.color", "#3498DB");
    }

    public boolean isDiscordHomeEnabled() {
        return config.getBoolean("discordsrv.events.home.enabled", true);
    }

    public String getDiscordHomeColor() {
        return config.getString("discordsrv.events.home.color", "#2ECC71");
    }

    public boolean isDiscordHomeDeleteEnabled() {
        return config.getBoolean("discordsrv.events.home_delete.enabled", true);
    }

    public String getDiscordHomeDeleteColor() {
        return config.getString("discordsrv.events.home_delete.color", "#E74C3C");
    }

    public boolean isRTPEnabled() {
        return config.getBoolean("rtp.enabled", true);
    }

    public boolean isRTPCommandRegistered() {
        return config.getBoolean("rtp.register-command", true);
    }

    public int getSpawnEntityMaxAmount() { return config.getInt("spawnentity.max-amount", 10);}

    public boolean isPlayerListLuckPermsEnabled() {
        return config.getBoolean("playerlist.show-luckperms-prefix", true);
    }

    public boolean isMotdEnabled() {
        return config.getBoolean("motd.enabled", true);
    }

    public List<String> getMotdLines() {
        return config.getStringList("motd.lines");
    }

    public boolean isBackupEnabled() {
        return config.getBoolean("backup.enabled", true);
    }

    public int getBackupKeepLast() {
        return config.getInt("backup.keep-last", 10);
    }

    public boolean isBackupOnShutdown() {
        return config.getBoolean("backup.on-shutdown", true);
    }

    public boolean isDiscordBanIpEnabled() {
        return config.getBoolean("discordsrv.events.banip.enabled", true);
    }

    public String getDiscordBanIpColor() {
        return config.getString("discordsrv.events.banip.color", "#E74C3C");
    }

    public boolean isSellEnabled() {
        return config.getBoolean("sell.enabled", true);
    }

    public String getSellGUITitle() {
        return config.getString("sell.gui.title", "<gradient:#06FFA5:#FFE66D>Sell Items</gradient>");
    }

    public int getSellGUISize() {
        return config.getInt("sell.gui.size", 54);
    }

    public boolean isSellGUIFillEmpty() {
        return config.getBoolean("sell.gui.fill-empty", true);
    }

    public String getSellGUIFillMaterial() {
        return config.getString("sell.gui.fill-material", "BLACK_STAINED_GLASS_PANE");
    }

    public String getSellGUIBorderMaterial() {
        return config.getString("sell.gui.border-material", "GRAY_STAINED_GLASS_PANE");
    }

    public boolean isSellGUISounds() {
        return config.getBoolean("sell.gui.sounds", true);
    }

    public String getFallbackLanguage() {
        return config.getString("fallback-language", "en_US");
    }

    public boolean isFlyPersistent() {
        return config.getBoolean("fly.persistent", true);
    }

    public boolean isFlyRestoreOnJoin() {
        return config.getBoolean("fly.restore-on-join", true);
    }

    public boolean isFlyDisableOnJoin() {
        return config.getBoolean("fly.disable-on-join", false);
    }

    public String getBroadcastPrefix() {
        return config.getString("broadcast.prefix", "<red><bold>[!]</bold></red> ");
    }

    public boolean isJoinLeaveHideVanished() {
        return config.getBoolean("join-leave-messages.hide-vanished", true);
    }

    public String getFirstJoinMessage() {
        return config.getString("join-leave-messages.first-join", "<yellow><player> joined for the first time!");
    }

    public boolean isSpawnTeleportOnRespawn() {
        return config.getBoolean("spawn.teleport-on-respawn", true);
    }

    public boolean isSpawnAllowBedsToOverride() {
        return config.getBoolean("spawn.allow-beds-to-override", true);
    }

    public boolean isChatSystemEnabled() {
        return config.getBoolean("chat.enabled", true);
    }

    public boolean isChatSlowModeEnabled() {
        return config.getBoolean("chat.slow-mode.enabled", false);
    }

    public int getChatSlowModeDelay() {
        return config.getInt("chat.slow-mode.delay-seconds", 3);
    }

    public double getChatCapslockThreshold() {
        return config.getDouble("chat.capslock-threshold", 0.7);
    }

    public boolean isChatStripColorsFromConsole() {
        return config.getBoolean("strip-colors-from-console", true);
    }

    public boolean isNickShowRealnameOnHover() {
        return config.getBoolean("nickname.show-realname-on-hover", true);
    }

    public String getNickHoverFormat() {
        return config.getString("nickname.hover-format", "<gray>Real name: <white><realname>");
    }

    public boolean isNickClickSuggestMsg() {
        return config.getBoolean("nickname.click-suggest-msg", true);
    }

    public String getChatLinkRemovalMessage() {
        return config.getString("chat.link-removal-message", "<red>[link removed]</red>");
    }

    public java.util.List<String> getChatLinkWhitelist() {
        return config.getStringList("chat.link-whitelist");
    }

    public boolean isChatMentionEnabled() {
        return config.getBoolean("chat.mention.enabled", true);
    }

    public String getChatMentionFormat() {
        return config.getString("chat.mention.format", "<yellow>@<player></yellow>");
    }

    public boolean isDiscordNickShowRealname() {
        return config.getBoolean("nickname.discord-show-realname", true);
    }

    public boolean isCurrencyBeforeAmount() {
        return config.getBoolean("economy.currency-before-amount", false);
    }

    public boolean isTrashEnabled() {
        return config.getBoolean("trash.enabled", true);
    }

    public boolean isTrashLogDisposals() {
        return config.getBoolean("trash.log-disposals", false);
    }

    public boolean isEnchantAllowUnsafe() {
        return config.getBoolean("enchant.allow-unsafe", false);
    }

    public long getRTPCooldown() {
        return config.getLong("rtp.cooldown", 60);
    }

    public long getRTPWarmup() {
        return config.getLong("rtp.warmup", 5);
    }

    public boolean isRTPCancelOnMovement() {
        return config.getBoolean("rtp.cancel-on-movement", true);
    }

    public boolean isRTPParticles() {
        return config.getBoolean("rtp.particles", true);
    }

    public int getRTPMaxAttempts() {
        return config.getInt("rtp.max-attempts", 10);
    }

    public int getRTPMinY() {
        return config.getInt("rtp.min-y", 64);
    }

    public int getRTPMaxY() {
        return config.getInt("rtp.max-y", 128);
    }

    public boolean isRTPUseWorldBorder() {
        return config.getBoolean("rtp.use-world-border", true);
    }

    public int getRTPGMinRadius(){
        return config.getInt("rtp.global.min-radius", 1000);
    }

    public int getRTPGMaxRadius(){
        return config.getInt("rtp.global.max-radius", 10000);
    }

    public int getScoreboardUpdateInterval(){
        return config.getInt("scoreboard.update-interval", 20);
    }

    public List<String> getGlobalBlockedBiomes (){
        return  config.getStringList("rtp.global.blocked-biomes");
    }

    public boolean isScoreboardPersistenceEnabled(){
        return config.getBoolean("scoreboard.persistent", true);
    }

    public String getRawScoreboardTitle(){
        return config.getString("scoreboard.title", "<gold><bold>MyServer</bold></gold>");
    }

    public List<String> getScoreboardstringList(){
        return config.getStringList("scoreboard.lines");
    }

}