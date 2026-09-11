package net.godlycow.org.essc.bootstrap.registrar;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.plugin.listener.EnderSeeListener;
import net.godlycow.org.essc.plugin.listener.InvseeListener;
import net.godlycow.org.essc.plugin.listener.JoinLeaveListener;
import net.godlycow.org.essc.plugin.listener.MuteListener;
import net.godlycow.org.essc.plugin.listener.ServerListPingListener;
import net.godlycow.org.essc.plugin.listener.VanishTabCompleteListener;
import net.godlycow.org.essc.util.VersionCheckUtil;

public class ListenerRegistrar {

    public ListenerRegistrar(EssentialsC plugin) {
        InvseeListener invseeListener = new InvseeListener(plugin);
        plugin.setInvseeListener(invseeListener);
        plugin.getServer().getPluginManager().registerEvents(invseeListener, plugin);

        JoinLeaveListener joinLeaveListener = new JoinLeaveListener(plugin);
        plugin.joinLeaveListener = joinLeaveListener;
        plugin.getServer().getPluginManager().registerEvents(joinLeaveListener, plugin);

        plugin.getServer().getPluginManager().registerEvents(new EnderSeeListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MuteListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new VersionCheckUtil(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new VanishTabCompleteListener(plugin), plugin);

        if (plugin.getConfigManager().isVanishHideFromServerList()) {
            plugin.getServer().getPluginManager().registerEvents(new ServerListPingListener(plugin), plugin);
        }
    }
}