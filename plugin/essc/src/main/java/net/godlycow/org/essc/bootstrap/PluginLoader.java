package net.godlycow.org.essc.bootstrap;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.afk.AFKManager;
import net.godlycow.org.essc.api.APIProvider;
import net.godlycow.org.essc.api.impl.EssentialsCAPIImpl;
import net.godlycow.org.essc.modules.auction.AhSoundManager;
import net.godlycow.org.essc.modules.auction.AuctionManager;
import net.godlycow.org.essc.modules.auction.gui.AhGuiManager;
import net.godlycow.org.essc.modules.back.BackManager;
import net.godlycow.org.essc.modules.backup.BackupManager;
import net.godlycow.org.essc.integration.bedrock.BedrockUtil;
import net.godlycow.org.essc.integration.bedrock.FloodgateHook;
import net.godlycow.org.essc.bootstrap.registrar.CommandRegistrar;
import net.godlycow.org.essc.bootstrap.registrar.EconomyRegistrar;
import net.godlycow.org.essc.bootstrap.registrar.ListenerRegistrar;
import net.godlycow.org.essc.integration.metrics.bstats.EconomyCharts;
import net.godlycow.org.essc.integration.metrics.bstats.UsageCharts;
import net.godlycow.org.essc.modules.chat.ChatManager;
import net.godlycow.org.essc.command.auction.AhCommand;
import net.godlycow.org.essc.integration.discord.DiscordSRVHook;
import net.godlycow.org.essc.integration.metrics.faststats.FastStatsManager;
import net.godlycow.org.essc.modules.fly.FlyManager;
import net.godlycow.org.essc.modules.fly.FlyMigration;
import net.godlycow.org.essc.modules.punishment.IpHistoryMigration;
import net.godlycow.org.essc.plugin.gui.GuiFramework;
import net.godlycow.org.essc.modules.home.HomeManager;
import net.godlycow.org.essc.modules.home.HomeNotificationManager;
import net.godlycow.org.essc.storage.user.UserManager;
import net.godlycow.org.essc.modules.kit.KitManager;
import net.godlycow.org.essc.language.HelpManager;
import net.godlycow.org.essc.language.LanguageManager;
import net.godlycow.org.essc.plugin.listener.AhListener;
import net.godlycow.org.essc.plugin.listener.BanListener;
import net.godlycow.org.essc.plugin.listener.WarpListener;
import net.godlycow.org.essc.modules.MOTDManager;
import net.godlycow.org.essc.modules.ReplyManager;
import net.godlycow.org.essc.modules.nick.NickManager;
import net.godlycow.org.essc.integration.placeholderapi.PlaceholderHook;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import net.godlycow.org.essc.modules.rtp.RTPGuiManager;
import net.godlycow.org.essc.modules.rtp.RTPManager;
import net.godlycow.org.essc.modules.RulesManager;
import net.godlycow.org.essc.modules.scoreboard.ScoreboardManager;
import net.godlycow.org.essc.modules.shop.ShopGuiManager;
import net.godlycow.org.essc.modules.shop.ShopListener;
import net.godlycow.org.essc.modules.shop.ShopManager;
import net.godlycow.org.essc.modules.shop.ShopSoundManager;
import net.godlycow.org.essc.modules.shop.sell.SellListener;
import net.godlycow.org.essc.modules.shop.sell.SellManager;
import net.godlycow.org.essc.server.software.ServerSoftware;
import net.godlycow.org.essc.modules.SpawnManager;
import net.godlycow.org.essc.modules.tab.TabManager;
import net.godlycow.org.essc.modules.teleport.TPAManager;
import net.godlycow.org.essc.util.StartupBanner;
import net.godlycow.org.essc.modules.VanishManager;
import net.godlycow.org.essc.modules.warp.WarpManager;
import org.bstats.bukkit.Metrics;

public final class PluginLoader {

    private final EssentialsC plugin;
    private final StartupTimer timer = new StartupTimer();

    public PluginLoader(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            load();
        } catch (Exception ex) {
            CrashHandler.handle(plugin, ex);
        }
    }

    private void load() {
        timer.start();
        StartupBanner.print(plugin, plugin.getLogger());
        timer.mark("banner");
        loadLanguages();
        timer.mark("languages");
        registerAPI();
        timer.mark("api");
        startPlugin();
        timer.mark("plugin");
        startMetrics();
        timer.mark("metrics");
        registerPlaceholderAPI();
        timer.mark("placeholderapi");
        String timings = timer.finish();
        plugin.debug("EssentialsC enabled — " + timings);
    }

    private void loadLanguages() {
        saveResourceIfAbsent("lang/en_US.json");
        saveResourceIfAbsent("lang/de_DE.json");

        LanguageManager languageManager = new LanguageManager(plugin);
        languageManager.load(plugin.getConfigManager().getDefaultLanguage());
        plugin.setLanguageManager(languageManager);

        HelpManager helpManager = new HelpManager(plugin);
        helpManager.load(plugin.getConfigManager().getDefaultLanguage());
        plugin.setHelpManager(helpManager);
    }


    private void registerAPI() {
        EssentialsCAPIImpl apiImpl = new EssentialsCAPIImpl(plugin);
        plugin.setApiImplementation(apiImpl);
        APIProvider.register(apiImpl);
    }

    private void startPlugin() {
        if (plugin.getConfigManager().isBackupEnabled()) {
            plugin.setBackupManager(new BackupManager(plugin));
        }

        if (plugin.getConfigManager().isMotdEnabled()) {
            plugin.setMotdManager(new MOTDManager(plugin));
        }

        plugin.setTpaManager(new TPAManager(plugin));
        plugin.setHomeManager(new HomeManager(plugin));
        plugin.setHomeNotificationManager(new HomeNotificationManager(plugin));
        plugin.setSpawnManager(new SpawnManager(plugin));
        plugin.setBackManager(new BackManager(plugin));
        plugin.setKitManager(new KitManager(plugin));
        plugin.setVanishManager(new VanishManager(plugin));
        plugin.setReplyManager(new ReplyManager());
        if (plugin.getConfigManager().isChatSystemEnabled()) {
            plugin.setChatManager(new ChatManager(plugin));
        } else {
            plugin.getLogger().info("Chat system is disabled in config.");
        }
        plugin.setUserManager(new UserManager(plugin));
        plugin.setPunishmentManager(new PunishmentManager(plugin));
        plugin.setFlyManager(new FlyManager(plugin));
        new FlyMigration(plugin).runIfNeeded();
        new IpHistoryMigration(plugin).runIfNeeded();

        RulesManager rulesManager = new RulesManager(plugin);
        rulesManager.load();
        plugin.setRulesManager(rulesManager);

        FloodgateHook floodgateHook = new FloodgateHook(plugin);
        plugin.setBedrockUtil(new BedrockUtil(plugin, floodgateHook));

        if (plugin.getConfigManager().isScoreboardEnabled()) {
            if (ServerSoftware.isFolia()) {
                plugin.getLogger().warning("Scoreboard feature is not *yet* supported on Folia.");
            } else {
                plugin.setScoreboardManager(new ScoreboardManager(plugin));
            }
        }

        if (plugin.getConfigManager().isLuckPermsTabEnabled() || plugin.getConfigManager().isNickEnabled()) {
            plugin.setTabManager(new TabManager(plugin));
        }

        if (plugin.getConfigManager().isNickEnabled()) {
            plugin.setNickManager(new NickManager(plugin));
        }

        if (plugin.getConfigManager().isRTPEnabled()) {
            RTPManager rtpManager = new RTPManager(plugin);
            plugin.setRtpManager(rtpManager);
            plugin.setRtpGuiManager(new RTPGuiManager(plugin, rtpManager));
        }

        if (plugin.getConfigManager().isEconomyEnabled()) {
            new EconomyRegistrar(plugin).enable();
        }

        if (plugin.getEconomyManager() != null) {
            plugin.getServer().getPluginManager().registerEvents(plugin.getEconomyManager(), plugin);
        }

        GuiFramework guiFramework = null;
        if (plugin.getConfigManager().isAHEnabled() || plugin.getConfigManager().isShopEnabled()
                || plugin.getConfigManager().isTrashEnabled() || plugin.getConfigManager().isKitGuiMode()) {
            guiFramework = new GuiFramework(plugin);
            guiFramework.loadTemplates();
            plugin.setGuiFramework(guiFramework);
        }

        if (plugin.getConfigManager().isKitGuiMode() && guiFramework != null) {
            plugin.setKitGuiManager(new net.godlycow.org.essc.modules.kit.gui.KitGuiManager(plugin, guiFramework));
        }

        if (plugin.getConfigManager().isAHEnabled()) {
            AuctionManager auctionManager = new AuctionManager(plugin);
            plugin.setAuctionManager(auctionManager);

            AhGuiManager ahGuiManager = new AhGuiManager(plugin, guiFramework, new AhSoundManager(plugin));
            plugin.setAhGuiManager(ahGuiManager);

            new AhListener(plugin, new AhCommand(plugin, ahGuiManager));
        }

        if (plugin.getConfigManager().isShopEnabled()) {
            ShopManager shopManager = new ShopManager(plugin);
            plugin.setShopManager(shopManager);

            ShopSoundManager shopSounds = new ShopSoundManager(plugin);
            ShopListener shopListener = new ShopListener(plugin, shopManager, shopSounds);
            shopManager.setShopListener(shopListener);
            plugin.getServer().getPluginManager().registerEvents(shopListener, plugin);

            if (guiFramework != null) {
                shopManager.setShopGuiManager(new ShopGuiManager(plugin, guiFramework, shopManager, shopSounds));
            }
        }

        if (plugin.getConfigManager().isWarpEnabled()) {
            plugin.setWarpManager(new WarpManager(plugin));
            plugin.getServer().getPluginManager().registerEvents(new WarpListener(plugin), plugin);
        }

        if (plugin.getConfigManager().isAfkEnabled()) {
            plugin.setAfkManager(new AFKManager(plugin));
        }

        if (plugin.getConfigManager().isDiscordSRVEnabled()) {
            DiscordSRVHook discordSRVHook = new DiscordSRVHook(plugin);
            discordSRVHook.init();
            plugin.setDiscordSRVHook(discordSRVHook);
        }

        if (plugin.getConfigManager().isSellEnabled()) {
            SellListener sellListener = new SellListener(plugin);
            SellManager sellManager = new SellManager(plugin, sellListener);
            sellListener.setSellManager(sellManager);
            plugin.setSellManager(sellManager);
            plugin.getServer().getPluginManager().registerEvents(sellListener, plugin);
        }

        new FirstRunHandler(plugin);

        new ListenerRegistrar(plugin);
        plugin.getServer().getPluginManager().registerEvents(new BanListener(plugin, plugin.getPunishmentManager()), plugin);
        plugin.getServer().getPluginManager().registerEvents(plugin, plugin);

        new CommandRegistrar(plugin).registerAll();
    }

    private void startMetrics() {
        int pluginId = 29401;
        Metrics metrics = new Metrics(plugin, pluginId);
        plugin.debug("bStats Metrics initialized.");

        if (plugin.getConfigManager().isEconomyEnabled()) {
            EconomyCharts.register(plugin, metrics);
        }

        UsageCharts.register(plugin, metrics);

        plugin.getFastStatsManager().ready();
    }

    private void registerPlaceholderAPI() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.debug("PlaceholderAPI not found, skipping placeholder registration.");
            return;
        }

        PlaceholderHook placeholderHook = new PlaceholderHook(plugin);
        if (placeholderHook.register()) {
            plugin.debug("PlaceholderAPI hook registered successfully.");
        } else {
            plugin.getLogger().warning("Failed to register PlaceholderAPI hook.");
        }
    }

    private void saveResourceIfAbsent(String resourcePath) {
        java.io.File target = new java.io.File(plugin.getDataFolder(), resourcePath);
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }
}