package net.godlycow.org.essc.api.impl.home;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.home.Home;
import net.godlycow.org.essc.api.home.HomeManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HomeManagerImpl implements HomeManager {
    private final EssentialsC plugin;

    public HomeManagerImpl(EssentialsC plugin) {
        this.plugin = plugin;
    }

    private net.godlycow.org.essc.modules.home.HomeManager internal() {
        return plugin.getHomeManager();
    }

    @Override
    public boolean isHomeSystemEnabled() {
        return internal() != null;
    }

    @Override
    public CompletableFuture<Home> fetchHome(UUID owner, String name) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return CompletableFuture.completedFuture(null);
        }
        return manager.getHome(owner, name).thenApply(home -> {
            if (home == null) {
                return null;
            }
            return (Home) new HomeImpl(home);
        });
    }

    @Override
    public CompletableFuture<List<Home>> fetchHomes(UUID owner) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return CompletableFuture.completedFuture(java.util.Collections.emptyList());
        }
        return manager.getHomes(owner).thenApply(homes -> {
            List<Home> result = new ArrayList<>();
            for (var home : homes) {
                result.add(new HomeImpl(home));
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<Boolean> homeExists(UUID owner, String name) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return CompletableFuture.completedFuture(false);
        }
        return manager.homeExists(owner, name);
    }

    @Override
    public CompletableFuture<Integer> getHomeCount(UUID owner) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return CompletableFuture.completedFuture(0);
        }
        return manager.getHomeCount(owner);
    }

    @Override
    public CompletableFuture<Boolean> setHome(Player player, String name, Location location) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return CompletableFuture.completedFuture(false);
        }
        return manager.setHome(player, name, location);
    }

    @Override
    public CompletableFuture<Boolean> setHome(UUID owner, String name, Location location) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return CompletableFuture.completedFuture(false);
        }
        return manager.setHome(owner, name, location);
    }

    @Override
    public CompletableFuture<Boolean> deleteHome(UUID owner, String name) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return CompletableFuture.completedFuture(false);
        }
        return manager.deleteHome(owner, name);
    }

    @Override
    public int getMaxHomes(Player player) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return 0;
        }
        return manager.getMaxHomes(player);
    }

    @Override
    public Collection<String> getCachedHomeNames(UUID owner) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return java.util.Collections.emptyList();
        }
        return manager.getCachedHomeNames(owner);
    }

    @Override
    public void clearCache(UUID owner) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return;
        }
        manager.clearCache(owner);
    }

    @Override
    public boolean isOnCooldown(Player player) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return false;
        }
        return manager.isOnCooldown(player);
    }

    @Override
    public long getRemainingCooldownSeconds(Player player) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return 0;
        }
        return manager.getRemainingCooldown(player);
    }

    @Override
    public boolean hasPendingTeleport(Player player) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return false;
        }
        return manager.hasPendingTeleport(player);
    }

    @Override
    public void cancelTeleport(Player player) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return;
        }
        manager.cancelTeleport(player);
    }

    @Override
    public void startTeleport(Player player, Home home) {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return;
        }
        manager.startTeleport(player, ((HomeImpl) home).getInternalHome());
    }

    @Override
    public void reload() {
        net.godlycow.org.essc.modules.home.HomeManager manager = internal();
        if (manager == null) {
            return;
        }
        manager.reload();
    }
}
