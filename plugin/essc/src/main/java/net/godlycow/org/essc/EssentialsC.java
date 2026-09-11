package net.godlycow.org.essc;

import net.godlycow.org.essc.api.impl.EssentialsCAPIImpl;
import net.godlycow.org.essc.bootstrap.PluginLoader;
import net.godlycow.org.essc.bootstrap.PluginShutdown;
import net.godlycow.org.essc.bootstrap.registrar.EconomyRegistrar;
import net.godlycow.org.essc.command.CommandCooldownManager;
import net.godlycow.org.essc.command.item.HatCommand;
import net.godlycow.org.essc.command.player.RenameCommand;
import net.godlycow.org.essc.integration.bedrock.BedrockUtil;
import net.godlycow.org.essc.integration.discord.DiscordSRVHook;
import net.godlycow.org.essc.integration.metrics.faststats.FastStatsManager;
import net.godlycow.org.essc.language.HelpManager;
import net.godlycow.org.essc.language.LanguageManager;
import net.godlycow.org.essc.modules.MOTDManager;
import net.godlycow.org.essc.modules.ReplyManager;
import net.godlycow.org.essc.modules.RulesManager;
import net.godlycow.org.essc.modules.SpawnManager;
import net.godlycow.org.essc.modules.VanishManager;
import net.godlycow.org.essc.modules.afk.AFKManager;
import net.godlycow.org.essc.modules.auction.AuctionManager;
import net.godlycow.org.essc.modules.auction.gui.AhGuiManager;
import net.godlycow.org.essc.modules.back.BackManager;
import net.godlycow.org.essc.modules.backup.BackupManager;
import net.godlycow.org.essc.modules.chat.ChatManager;
import net.godlycow.org.essc.modules.fly.FlyManager;
import net.godlycow.org.essc.modules.home.HomeManager;
import net.godlycow.org.essc.modules.home.HomeNotificationManager;
import net.godlycow.org.essc.modules.kit.KitManager;
import net.godlycow.org.essc.modules.kit.gui.KitGuiManager;
import net.godlycow.org.essc.modules.nick.NickManager;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import net.godlycow.org.essc.modules.rtp.RTPGuiManager;
import net.godlycow.org.essc.modules.rtp.RTPManager;
import net.godlycow.org.essc.modules.scoreboard.ScoreboardManager;
import net.godlycow.org.essc.modules.shop.ShopManager;
import net.godlycow.org.essc.modules.shop.sell.SellManager;
import net.godlycow.org.essc.modules.tab.TabManager;
import net.godlycow.org.essc.modules.teleport.TPAManager;
import net.godlycow.org.essc.modules.warp.WarpManager;
import net.godlycow.org.essc.plugin.config.CommandsConfig;
import net.godlycow.org.essc.plugin.config.EssConfig;
import net.godlycow.org.essc.plugin.economy.EconomyManager;
import net.godlycow.org.essc.plugin.economy.VaultHook;
import net.godlycow.org.essc.plugin.gui.GuiFramework;
import net.godlycow.org.essc.plugin.listener.InvseeListener;
import net.godlycow.org.essc.plugin.listener.JoinLeaveListener;
import net.godlycow.org.essc.storage.user.UserManager;
import net.godlycow.org.essc.storage.user.UserProfile;
import net.godlycow.org.essc.util.TeleportHelper;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class EssentialsC extends JavaPlugin implements Listener {
    private final TeleportHelper teleportHelper = new TeleportHelper(this);
    private EssConfig essConfig;
    private CommandsConfig commandsConfig;
    private LanguageManager languageManager;
    private HelpManager helpManager;
    private EconomyManager economyManager;
    private VaultHook vaultHook;
    private TPAManager tpaManager;
    private HomeManager homeManager;
    private HomeNotificationManager homeNotificationManager;
    private SpawnManager spawnManager;
    private BackManager backManager;
    private KitManager kitManager;
    private KitGuiManager kitGuiManager;
    private VanishManager vanishManager;
    private ScoreboardManager scoreboardManager;
    private EconomyRegistrar economyRegistrar;
    private ShopManager shopManager;
    private NickManager nickManager;
    private PunishmentManager punishmentManager;
    private ReplyManager replyManager;
    private AuctionManager auctionManager;
    private WarpManager warpManager;
    private AFKManager afkManager;
    private ChatManager chatManager;
    private DiscordSRVHook discordSRVHook;
    private RTPManager rtpManager;
    private RTPGuiManager rtpGuiManager;
    private TabManager tabManager;
    private FlyManager flyManager;
    private BedrockUtil bedrockUtil;
    private RulesManager rulesManager;
    private MOTDManager motdManager;
    private BackupManager backupManager;
    private UserManager userManager;
    private SellManager sellManager;
    private AhGuiManager ahGuiManager;
    private EssentialsCAPIImpl apiImplementation;
    private HatCommand hatCommand;
    public JoinLeaveListener joinLeaveListener;
    private RenameCommand renameCommand;
    private GuiFramework guiFramework;
    private CommandCooldownManager commandCooldownManager;
    private InvseeListener invseeListener;

    private final FastStatsManager fastStatsManager = new FastStatsManager(this);
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onLoad() {
        saveDefaultConfig();

        essConfig = new EssConfig(this);
        essConfig.migrate();

        commandsConfig = new CommandsConfig(this);
        commandsConfig.load();

        commandCooldownManager = new CommandCooldownManager();


        if (essConfig.isEconomyEnabled()) {
            debug("Economy is enabled, initializing EconomyManager...");
            economyManager = new EconomyManager(this);
            try {
                Class.forName("net.milkbowl.vault.economy.Economy");
                vaultHook = new VaultHook(economyManager);
                if (vaultHook.hook()) {
                    getLogger().info("Successfully hooked into Vault.");
                } else {
                    getLogger().info("Vault not found, skipping Vault economy registration.");
                }
            } catch (ClassNotFoundException e) {
                getLogger().info("Vault not found, skipping Vault economy registration.");
                vaultHook = null;
            }
        } else {
            debug("Economy is disabled, skipping initialization.");
        }
    }

    @Override
    public void onEnable() {
        new PluginLoader(this).start();
    }

    @Override
    public void onDisable() {
        new PluginShutdown(this).stop();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (getUserManager() != null) { //Set player language
            getUserManager().loadProfile(player.getUniqueId(), player.getName()).thenAccept(profile -> {
                String lang = profile.getLanguageCode();
                if (lang != null && !lang.isEmpty()) {
                    getLanguageManager().setPlayerLanguage(player.getUniqueId(), lang);
                }
            });
        }
        getHomeManager().getHomes(player.getUniqueId());
        if (homeNotificationManager != null) {
            homeNotificationManager.deliverPending(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (getUserManager() != null) {
            UUID uuid = event.getPlayer().getUniqueId();
            UserProfile profile = getUserManager().getCachedProfile(uuid);
            if (profile != null) {
                getUserManager().saveAsync(profile).thenRun(() -> getUserManager().clearCache(uuid));
            } else {
                getUserManager().clearCache(uuid);
            }
        }
        getHomeManager().clearCache(event.getPlayer().getUniqueId());
    }

    public void debug(String message) {
        if (essConfig != null && essConfig.isDebug()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public TeleportHelper teleportHelper() {
        return teleportHelper;
    }

    public FastStatsManager getFastStatsManager() {
        return fastStatsManager;
    }

    public EssConfig getConfigManager() {
        return essConfig;
    }

    public CommandsConfig getCommandsConfig() {
        return commandsConfig;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public void setLanguageManager(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public HelpManager getHelpManager() {
        return helpManager;
    }

    public void setHelpManager(HelpManager helpManager) {
        this.helpManager = helpManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public void setEconomyManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    public void setVaultHook(VaultHook vaultHook) {
        this.vaultHook = vaultHook;
    }

    public boolean isVaultHooked() {
        return vaultHook != null && vaultHook.isHooked();
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public TPAManager getTPAManager() {
        return tpaManager;
    }

    public void setTpaManager(TPAManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public void setHomeManager(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    public HomeNotificationManager getHomeNotificationManager() {
        return homeNotificationManager;
    }

    public void setHomeNotificationManager(HomeNotificationManager homeNotificationManager) {
        this.homeNotificationManager = homeNotificationManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public void setSpawnManager(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    public BackManager getBackManager() {
        return backManager;
    }

    public void setBackManager(BackManager backManager) {
        this.backManager = backManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public void setKitManager(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    public net.godlycow.org.essc.modules.kit.gui.KitGuiManager getKitGuiManager() {
        return kitGuiManager;
    }

    public void setKitGuiManager(net.godlycow.org.essc.modules.kit.gui.KitGuiManager kitGuiManager) {
        this.kitGuiManager = kitGuiManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public void setVanishManager(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public void setScoreboardManager(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    public EconomyRegistrar getEconomyRegistrar() {
        return economyRegistrar;
    }

    public NickManager getNickManager() {
        return nickManager;
    }

    public void setNickManager(NickManager nickManager) {
        this.nickManager = nickManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public void setShopManager(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public void setPunishmentManager(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }


    public ReplyManager getReplyManager() {
        return replyManager;
    }

    public void setReplyManager(ReplyManager replyManager) {
        this.replyManager = replyManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public void setAuctionManager(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public void setWarpManager(WarpManager warpManager) {
        this.warpManager = warpManager;
    }

    public AFKManager getAfkManager() {
        return afkManager;
    }

    public void setAfkManager(AFKManager afkManager) {
        this.afkManager = afkManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public void setChatManager(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public DiscordSRVHook getDiscordSRVHook() {
        return discordSRVHook;
    }

    public void setDiscordSRVHook(DiscordSRVHook discordSRVHook) {
        this.discordSRVHook = discordSRVHook;
    }

    public RTPManager getRtpManager() {
        return rtpManager;
    }

    public void setRtpManager(RTPManager rtpManager) {
        this.rtpManager = rtpManager;
    }

    public RTPGuiManager getRtpGuiManager() {
        return rtpGuiManager;
    }

    public void setRtpGuiManager(RTPGuiManager rtpGuiManager) {
        this.rtpGuiManager = rtpGuiManager;
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    public void setTabManager(TabManager tabManager) {
        this.tabManager = tabManager;
    }

    public FlyManager getFlyManager() {
        return flyManager;
    }

    public void setFlyManager(FlyManager flyManager) {
        this.flyManager = flyManager;
    }

    public BedrockUtil getBedrockUtil() {
        return bedrockUtil;
    }

    public void setBedrockUtil(BedrockUtil bedrockUtil) {
        this.bedrockUtil = bedrockUtil;
    }

    public RulesManager getRulesManager() {
        return rulesManager;
    }

    public void setRulesManager(RulesManager rulesManager) {
        this.rulesManager = rulesManager;
    }

    public MOTDManager getMotdManager() {
        return motdManager;
    }

    public void setMotdManager(MOTDManager motdManager) {
        this.motdManager = motdManager;
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }

    public void setBackupManager(BackupManager backupManager) {
        this.backupManager = backupManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public SellManager getSellManager() {
        return sellManager;
    }

    public void setSellManager(SellManager sellManager) {
        this.sellManager = sellManager;
    }

    public AhGuiManager getAhGuiManager() {
        return ahGuiManager;
    }

    public void setAhGuiManager(AhGuiManager ahGuiManager) {
        this.ahGuiManager = ahGuiManager;
    }

    public EssentialsCAPIImpl getApiImplementation() {
        return apiImplementation;
    }

    public void setApiImplementation(EssentialsCAPIImpl apiImplementation) {
        this.apiImplementation = apiImplementation;
    }

    public HatCommand getHatCommand() {
        return hatCommand;
    }

    public JoinLeaveListener getJoinLeaveListener() {
        return joinLeaveListener;
    }

    public RenameCommand getRenameCommand() {
        return renameCommand;
    }

    public GuiFramework getGuiFramework() {
        return guiFramework;
    }

    public void setGuiFramework(GuiFramework guiFramework) {
        this.guiFramework = guiFramework;
    }

    public CommandCooldownManager getCommandCooldownManager() {
        return commandCooldownManager;
    }

    public InvseeListener getInvseeListener() {
        return invseeListener;
    }

    public void setInvseeListener(InvseeListener invseeListener) {
        this.invseeListener = invseeListener;
    }

}