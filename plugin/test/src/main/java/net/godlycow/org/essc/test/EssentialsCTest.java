package net.godlycow.org.essc.test;

import org.bukkit.plugin.java.JavaPlugin;

public class EssentialsCTest extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("test-scoreboard").setExecutor(new ScoreboardDisableTestCommand());
        getCommand("test-ah").setExecutor(new AuctionDisableTestCommand());
        getLogger().info("EssentialsCTest enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("EssentialsCTest disabled");
    }
}
