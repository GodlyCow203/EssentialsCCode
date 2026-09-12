package net.godlycow.org.essc.bootstrap.registrar;

import net.godlycow.org.essc.CommandRegistration;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.command.EsscCommand;
import net.godlycow.org.essc.command.admin.*;
import net.godlycow.org.essc.command.afk.AFKCommand;
import net.godlycow.org.essc.command.afk.AFKListCommand;
import net.godlycow.org.essc.command.auction.AhCommand;
import net.godlycow.org.essc.command.entity.SpawnEntityCommand;
import net.godlycow.org.essc.command.home.DelHomeCommand;
import net.godlycow.org.essc.command.home.HomeCommand;
import net.godlycow.org.essc.command.home.HomesCommand;
import net.godlycow.org.essc.command.home.SetHomeCommand;
import net.godlycow.org.essc.command.inv.*;
import net.godlycow.org.essc.command.item.EnchantCommand;
import net.godlycow.org.essc.command.item.HatCommand;
import net.godlycow.org.essc.command.item.ItemIdCommand;
import net.godlycow.org.essc.command.item.UnenchantCommand;
import net.godlycow.org.essc.command.kit.KitCommand;
import net.godlycow.org.essc.command.kit.KitsCommand;
import net.godlycow.org.essc.command.player.*;
import net.godlycow.org.essc.command.server.BroadcastCommand;
import net.godlycow.org.essc.command.server.UptimeCommand;
import net.godlycow.org.essc.command.spawn.SetSpawnCommand;
import net.godlycow.org.essc.command.spawn.SpawnCommand;
import net.godlycow.org.essc.command.tpa.*;
import net.godlycow.org.essc.command.warp.*;
import net.godlycow.org.essc.language.LanguageCommand;
import net.godlycow.org.essc.migration.MigrationCommand;
import net.godlycow.org.essc.modules.auction.gui.AhGuiManager;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import net.godlycow.org.essc.plugin.config.CommandsConfig;
import org.bukkit.command.PluginCommand;

import java.util.List;

public class CommandRegistrar {

    private final EssentialsC plugin;
    private final PunishmentManager punishmentManager;
    private final CommandsConfig commandsConfig;
    private final AhGuiManager ahGuiManager;


    public CommandRegistrar(EssentialsC plugin) {
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
        this.commandsConfig = plugin.getCommandsConfig();
        this.ahGuiManager = plugin.getAhGuiManager();
    }

    public void registerAll() {
        registerCommands();
        CommandRegistration.syncCommands();
    }

    private void registerCommands() {
        GamemodeCommand gamemodeCommand = new GamemodeCommand(plugin);

        register("heal",           new HealCommand(plugin));
        register("essc",           new EsscCommand(plugin));
        register("feed",           new FeedCommand(plugin));
        register("ping",           new PingCommand(plugin));
        register("fly",            new FlyCommand(plugin));
        register("god",            new GodCommand(plugin));
        register("vanish",         new VanishCommand(plugin));
        register("repair",         new RepairCommand(plugin));
        register("rename",         new RenameCommand(plugin));

        if (plugin.getConfigManager().isScoreboardEnabled()) {
            register("scoreboard", new ScoreboardCommand(plugin));
        } else {
            CommandRegistration.unregisterCommand("scoreboard");
            CommandRegistration.unregisterCommand("essentialsc:scoreboard");
            plugin.debug("Scoreboard command unregistered (scoreboard.enabled is false)");
        }

        register("tpa",            new TPACommand(plugin));
        register("tpahere",        new TPAHereCommand(plugin));
        register("tpaccept",       new TPAcceptCommand(plugin));
        register("playerlist",     new PlayerListCommand(plugin));
        register("tpdeny",         new TPADenyCommand(plugin));
        register("tpcancel",       new TPACancelCommand(plugin));
        register("tpaignore",      new TPAIgnoreCommand(plugin));
        register("tpatoggle",      new TPAToggleCommand(plugin));
        register("tpaqueue",       new TPAQueueCommand(plugin));
        register("sethome",        new SetHomeCommand(plugin));
        register("itemid",         new ItemIdCommand(plugin));
        register("rules",          new RulesCommand(plugin));
        register("spawnentity",    new SpawnEntityCommand(plugin));
        register("home",           new HomeCommand(plugin));
        register("delhome",        new DelHomeCommand(plugin));
        register("homes",          new HomesCommand(plugin));
        register("spawn",          new SpawnCommand(plugin));
        register("setspawn",       new SetSpawnCommand(plugin));
        register("invsee",         new InvseeCommand(plugin));
        register("ban-ip",         new BanIpCommand(plugin, punishmentManager));
        register("tpoffline",      new TpOfflineCommand(plugin, plugin.getUserManager()));
        register("banlist",        new BanListCommand(plugin, punishmentManager));
        register("clearinventory", new ClearInventoryCommand(plugin));
        register("enderchest",     new EnderChestCommand(plugin));
        register("endersee",       new EnderSeeCommand(plugin));
        register("speed",          new SpeedCommand(plugin));
        register("anvil",          new AnvilCommand(plugin));
        register("craftingtable",  new CraftingTableCommand(plugin));
        register("back",           new BackCommand(plugin));
        register("dback",          new DBackCommand(plugin));
        register("tphereall",      new TPHereAllCommand(plugin));
        register("kit",            new KitCommand(plugin));
        register("kits",           new KitsCommand(plugin));
        register("playtime",       new PlaytimeCommand(plugin));
        register("uptime",         new UptimeCommand(plugin));
        register("tphere",         new TPHereCommand(plugin));
        register("broadcast",      new BroadcastCommand(plugin));
        register("enchant",        new EnchantCommand(plugin));
        register("unenchant",      new UnenchantCommand(plugin));
        register("hat",            new HatCommand(plugin));
        register("sudo",           new SudoCommand(plugin));
        register("kick",           new KickCommand(plugin));
        register("ban",            new BanCommand(plugin, punishmentManager));
        register("unban",          new UnbanCommand(plugin, punishmentManager));
        register("mute",           new MuteCommand(plugin, punishmentManager));
        register("unmute",         new UnmuteCommand(plugin, punishmentManager));
        register("checkpunish",    new CheckpunishCommand(plugin, punishmentManager));
        register("ignore",         new IgnoreCommand(plugin));
        register("msg",            new MsgCommand(plugin));
        register("reply",          new ReplyCommand(plugin));
        register("seen",           new SeenCommand(plugin));
        register("top",            new TopCommand(plugin));
        register("ptime",          new PtimeCommand(plugin));
        register("pweather",       new PweatherCommand(plugin));
        register("language",       new LanguageCommand(plugin));
        register("warp",           new WarpCommand(plugin));
        register("setwarp",        new SetWarpCommand(plugin));
        register("delwarp",        new DelWarpCommand(plugin));
        register("warps",          new WarpsCommand(plugin));
        register("warpadmin",      new WarpAdminCommand(plugin));
        register("afk",            new AFKCommand(plugin));
        register("afklist",        new AFKListCommand(plugin));
        register("migration",      new MigrationCommand(plugin));
        register("user",           new UserCommand(plugin));
        register("suicide",        new SuicideCommand(plugin));
        register("smite",          new SmiteCommand(plugin));
        register("gm",             gamemodeCommand);
        register("gamemode",       gamemodeCommand);
        register("gms",            gamemodeCommand);
        register("gmc",            gamemodeCommand);
        register("gmsp",           gamemodeCommand);
        register("gma",            gamemodeCommand);
        register("stonecutter",    new StonecutterCommand(plugin));
        register("unban-ip",       new UnbanIpCommand(plugin, punishmentManager));
        register("discord",        new DiscordCommand(plugin));




        if (plugin.getConfigManager().isNickEnabled()) {
            register("nick", new NickCommand(plugin));
            register("realname", new RealNameCommand(plugin));
        }

        if (plugin.getConfigManager().isShopEnabled()) {
            register("shop", new ShopCommand(plugin));
        } else {
            CommandRegistration.unregisterCommand("shop");
            CommandRegistration.unregisterCommand("essentialsc:shop");
            plugin.debug("Shop command unregistered (shop.enabled is false)");
        }

        if (plugin.getConfigManager().isRTPCommandRegistered()) {
            register("rtp", new RTPCommand(plugin));
        } else {
            CommandRegistration.unregisterCommand("rtp");
            CommandRegistration.unregisterCommand("essentialsc:rtp");
            plugin.debug("RTP command unregistered (rtp.register-command is false)");
        }

        if (plugin.getConfigManager().isSellEnabled()) {
            register("sell", new SellCommand(plugin));
            register("worth", new WorthCommand(plugin));
            register("quicksell", new QuickSellCommand(plugin));
        } else {
            CommandRegistration.unregisterCommand("sell");
            CommandRegistration.unregisterCommand("essentialsc:sell");
            CommandRegistration.unregisterCommand("worth");
            CommandRegistration.unregisterCommand("essentialsc:worth");
            CommandRegistration.unregisterCommand("quicksell");
            CommandRegistration.unregisterCommand("essentialsc:quicksell");
            plugin.debug("Sell commands unregistered (sell.enabled is false)");
        }

        if (plugin.getConfigManager().isAHEnabled()) {
            register("ah", new AhCommand(plugin, ahGuiManager));
        } else {
            CommandRegistration.unregisterCommand("ah");
            CommandRegistration.unregisterCommand("essentialsc:ah");
            plugin.debug("AH command unregistered (ah.enabled is false)");
        }

        if (plugin.getConfigManager().isTrashEnabled()) {
            register("trash", new TrashCommand(plugin, plugin.getGuiFramework()));

        } else {
            CommandRegistration.unregisterCommand("trash");
            CommandRegistration.unregisterCommand("essentialsc:trash");
            plugin.debug("Trash command unregistered ( trash.enabled is false");
        }
    }

    private void register(String name, Command command) {

        if (!commandsConfig.isEnabled(name)) {
            CommandRegistration.unregisterCommand(name);
            CommandRegistration.unregisterCommand("essentialsc:" + name);
            plugin.debug("Command '" + name + "' disabled in commands.yml – unregistered.");
            return;
        }

        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand == null) {
            plugin.getLogger().warning("Command '" + name + "' not found in plugin.yml – skipping.");
            return;
        }

        switch (commandsConfig.getPriority(name)) {
            case "override" -> {
                CommandRegistration.unregisterCommand(name);
                pluginCommand.setExecutor(command);
                pluginCommand.setTabCompleter(command);
                pluginCommand.register(plugin.getServer().getCommandMap());
                plugin.debug("Registered '" + name + "' with OVERRIDE priority.");
            }
            case "low" -> {
                if (CommandRegistration.isRegistered(name)) {
                    plugin.debug("Skipped '" + name + "' (LOW priority – already claimed).");
                    return;
                }
                pluginCommand.setExecutor(command);
                pluginCommand.setTabCompleter(command);
                plugin.debug("Registered '" + name + "' with LOW priority.");
            }
            default -> {
                pluginCommand.setExecutor(command);
                pluginCommand.setTabCompleter(command);
                plugin.debug("Registered '" + name + "' with NORMAL priority.");
            }
        }

        List<String> configAliases = commandsConfig.getAliases(name);
        for (String alias : configAliases) {
            CommandRegistration.registerAlias(alias, pluginCommand);
            plugin.debug("Registered alias '/" + alias + "' -> '" + name + "'");
        }
    }
}