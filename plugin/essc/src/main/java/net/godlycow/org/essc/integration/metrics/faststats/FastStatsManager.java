package net.godlycow.org.essc.integration.metrics.faststats;

import dev.faststats.ErrorTracker;
import dev.faststats.Metrics;
import dev.faststats.bukkit.BukkitContext;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public final class FastStatsManager {

    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware()
            .ignoreError(InvocationTargetException.class)
            .ignoreError(IOException.class, "Broken pipe");  //ignore IT + IO Exceptions
    private final BukkitContext context;

    public FastStatsManager(JavaPlugin plugin) {
        this.context = new BukkitContext.Factory(plugin, "753b5c694c676a97c8966eee8a159012")
                .errorTrackerService(ERROR_TRACKER)
                .metrics(Metrics.Factory::create)
                .create();
        if (context.getConfig().enabled()) {
            plugin.getLogger().info("Enabled FastStats Metrics and Error Tracking");
        }
    }

    public void ready() {
        context.ready();
    }

    public void shutdown() {
        context.shutdown();
    }
}