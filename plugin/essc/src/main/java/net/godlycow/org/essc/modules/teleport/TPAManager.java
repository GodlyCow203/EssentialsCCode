package net.godlycow.org.essc.modules.teleport;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TPAManager implements Listener {
    private final EssentialsC plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, List<TPARequest>> incomingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, List<TPARequest>> outgoingRequests = new ConcurrentHashMap<>();
    private final Set<UUID> teleporting = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> blockedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Set<UUID>> ignoredPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> warmupTasks = new ConcurrentHashMap<>();
    private long cooldownDuration;
    private long warmupDuration;
    private long timeoutDuration;
    private int maxPending;
    private int maxOutgoing;
    private boolean denyMovement;
    private boolean useParticles;
    private boolean useSounds;
    private List<String> blockedWorlds;

    public TPAManager(EssentialsC plugin) {
        this.plugin = plugin;
        loadConfig();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startCleanupTask();
        plugin.debug("TPA Manager initialized");
    }

    public void reload() {
        loadConfig();
        plugin.debug("TPA configuration reloaded");
    }

    private void loadConfig() {
        var cm = plugin.getConfigManager();
        this.cooldownDuration = cm.getTPACooldown() * 1000;
        this.warmupDuration = cm.getTPAWarmup() * 20;
        this.timeoutDuration = cm.getTPATimeout() * 1000;
        this.maxPending = cm.getTPAMaxPending();
        this.maxOutgoing = cm.getTPAMaxOutgoing();
        this.denyMovement = cm.isTPADenyMovement();

        this.useParticles = cm.isTPAParticles();
        this.useSounds = cm.isTPASounds();
        this.blockedWorlds = cm.getTPABlockedWorlds();
    }

    public boolean requestTeleport(Player requester, Player target, TPARequest.Type type) {
        if (blockedWorlds.contains(requester.getWorld().getName())) {
            requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.error.blocked_world"));
            return false;
        }

        if (blockedPlayers.contains(target.getUniqueId())) {
            requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.error.target_blocked",
                    Map.of("player", target.getName())));
            return false;
        }

        if (ignoredPlayers.getOrDefault(target.getUniqueId(), Collections.emptySet())
                .contains(requester.getUniqueId())) {
            requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.error.ignored",
                    Map.of("player", target.getName())));
            return false;
        }

        if (hasCooldown(requester)) {
            long remaining = (cooldowns.get(requester.getUniqueId()) - System.currentTimeMillis()) / 1000;
            requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.error.cooldown",
                    Map.of("seconds", String.valueOf(remaining))));
            return false;
        }

        cleanupExpiredForPlayer(requester);

        List<TPARequest> currentOutgoing = outgoingRequests.getOrDefault(requester.getUniqueId(), new ArrayList<>());
        if (currentOutgoing.size() >= maxOutgoing) {
            requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.error.max_outgoing"));
            return true;
        }

        List<TPARequest> targetIncoming = incomingRequests.getOrDefault(target.getUniqueId(), new ArrayList<>());
        targetIncoming.removeIf(r -> {
            if (r.isExpired(timeoutDuration)) {
                removeRequestFromBothMaps(r);
                return true;
            }
            return false;
        });

        if (targetIncoming.size() >= maxPending) {
            requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.error.target_busy",
                    Map.of("player", target.getName())));
            return false;
        }

        TPARequest request = new TPARequest(requester, target, type);

        incomingRequests.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>()).add(request);
        outgoingRequests.computeIfAbsent(requester.getUniqueId(), k -> new ArrayList<>()).add(request);

        String typeKey = type == TPARequest.Type.TPA ? "tpa" : "tpahere";

        requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.sent." + typeKey,
                Map.of("player", target.getName(), "seconds", String.valueOf(timeoutDuration / 1000))));

        target.sendMessage(plugin.getLanguageManager().get(target, "tpa.received." + typeKey,
                Map.of("player", requester.getName())));

        if (useSounds) {
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }

        plugin.debug(requester.getName() + " sent " + type + " request to " + target.getName());
        return true;
    }

    public boolean acceptRequest(Player target, Player requester) {
        List<TPARequest> requests = incomingRequests.get(target.getUniqueId());
        if (requests == null) return false;

        requests.removeIf(r -> {
            if (r.isExpired(timeoutDuration)) {
                notifyExpired(r);
                removeRequestFromBothMaps(r);
                return true;
            }
            return false;
        });

        TPARequest request = requests.stream()
                .filter(r -> r.getRequester().equals(requester.getUniqueId()))
                .findFirst()
                .orElse(null);

        if (request == null) return false;

        removeRequestFromBothMaps(request);

        Player toTeleport = request.getType() == TPARequest.Type.TPA ? requester : target;
        Player destination = request.getType() == TPARequest.Type.TPA ? target : requester;

        startTeleportWarmup(toTeleport, destination, request.getType());

        target.sendMessage(plugin.getLanguageManager().get(target, "tpa.accept.target",
                Map.of("player", requester.getName())));
        requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.accept.requester",
                Map.of("player", target.getName())));

        setCooldown(requester);

        return true;
    }

    public boolean denyRequest(Player target, Player requester) {
        List<TPARequest> requests = incomingRequests.get(target.getUniqueId());
        if (requests == null) return false;

        TPARequest request = requests.stream()
                .filter(r -> r.getRequester().equals(requester.getUniqueId()) && !r.isExpired(timeoutDuration))
                .findFirst()
                .orElse(null);

        if (request == null) return false;

        removeRequestFromBothMaps(request);

        target.sendMessage(plugin.getLanguageManager().get(target, "tpa.deny.target",
                Map.of("player", requester.getName())));
        requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.deny.requester",
                Map.of("player", target.getName())));

        return true;
    }

    public boolean cancelRequest(Player requester, Player target) {
        List<TPARequest> requests = outgoingRequests.get(requester.getUniqueId());
        if (requests == null) return false;

        TPARequest request = requests.stream()
                .filter(r -> r.getTarget().equals(target.getUniqueId()) && !r.isExpired(timeoutDuration))
                .findFirst()
                .orElse(null);

        if (request == null) return false;

        removeRequestFromBothMaps(request);

        requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.cancel.success",
                Map.of("player", target.getName())));

        return true;
    }

    public void toggleTPA(Player player) {
        UUID uuid = player.getUniqueId();
        if (blockedPlayers.contains(uuid)) {
            blockedPlayers.remove(uuid);
            player.sendMessage(plugin.getLanguageManager().get(player, "tpa.toggle.enabled"));
        } else {
            blockedPlayers.add(uuid);
            player.sendMessage(plugin.getLanguageManager().get(player, "tpa.toggle.disabled"));
        }
    }

    public void toggleIgnore(Player player, Player target) {
        Set<UUID> ignored = ignoredPlayers.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());

        if (ignored.contains(target.getUniqueId())) {
            ignored.remove(target.getUniqueId());
            player.sendMessage(plugin.getLanguageManager().get(player, "tpa.ignore.removed",
                    Map.of("player", target.getName())));
        } else {
            ignored.add(target.getUniqueId());
            player.sendMessage(plugin.getLanguageManager().get(player, "tpa.ignore.added",
                    Map.of("player", target.getName())));
        }
    }

    private void startTeleportWarmup(Player teleporter, Player destination, TPARequest.Type type) {
        if (warmupDuration <= 0) {
            executeTeleport(teleporter, destination);
            return;
        }

        teleporter.sendMessage(plugin.getLanguageManager().get(teleporter, "tpa.warmup",
                Map.of("seconds", String.valueOf(warmupDuration / 20))));

        teleporting.add(teleporter.getUniqueId());

        ScheduledTask particleTask = null;
        if (useParticles) {
            particleTask = teleporter.getScheduler().runAtFixedRate(plugin, task -> {
                if (!teleporter.isOnline() || !teleporting.contains(teleporter.getUniqueId())) {
                    warmupTasks.remove(teleporter.getUniqueId());
                    return;
                }
                teleporter.getWorld().spawnParticle(Particle.PORTAL, teleporter.getLocation().add(0, 1, 0), 10);
            }, null, 1L, 1L);
        }

        final ScheduledTask finalParticleTask = particleTask;

        ScheduledTask task = teleporter.getScheduler().runDelayed(plugin, task1 -> {
            if (teleporting.contains(teleporter.getUniqueId())) {
                executeTeleport(teleporter, destination);
            }
            warmupTasks.remove(teleporter.getUniqueId());
            if (finalParticleTask != null) finalParticleTask.cancel();
        }, null, warmupDuration);

        warmupTasks.put(teleporter.getUniqueId(), task);
    }

    private void executeTeleport(Player teleporter, Player destination) {
        teleporting.remove(teleporter.getUniqueId());

        if (!teleporter.isOnline() || !destination.isOnline()) return;

        Location from = teleporter.getLocation();
        Location dest = destination.getLocation();

        if (plugin.getBackManager() != null) {
            plugin.getBackManager().setBackLocation(teleporter, from);
        }

        plugin.teleportHelper().teleportAsync(teleporter, dest).thenAccept(success -> {
            if (!success) return;

            if (useSounds) {
                teleporter.playSound(teleporter.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                destination.playSound(destination.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }

            if (useParticles) {
                teleporter.getWorld().spawnParticle(Particle.PORTAL, teleporter.getLocation(), 100, 0.5, 1, 0.5);
            }

            teleporter.sendMessage(plugin.getLanguageManager().get(teleporter, "tpa.teleport.success"));
        });
    }

    public void cancelTeleport(Player player, String reason) {
        if (!teleporting.contains(player.getUniqueId())) return;

        teleporting.remove(player.getUniqueId());
        ScheduledTask task = warmupTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();

        String msgKey = switch (reason) {
            case "move" -> "tpa.teleport.cancelled.move";
            case "damage" -> "tpa.teleport.cancelled.damage";
            default -> "tpa.teleport.cancelled";
        };

        player.sendMessage(plugin.getLanguageManager().get(player, msgKey));
    }

    private void removeRequestFromBothMaps(TPARequest request) {
        incomingRequests.computeIfPresent(request.getTarget(), (k, v) -> {
            v.remove(request);
            return v.isEmpty() ? null : v;
        });
        outgoingRequests.computeIfPresent(request.getRequester(), (k, v) -> {
            v.remove(request);
            return v.isEmpty() ? null : v;
        });
        request.setExpired(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!denyMovement) return;

        Player player = event.getPlayer();
        if (!teleporting.contains(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            cancelTeleport(player, "move");
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!teleporting.contains(player.getUniqueId())) return;

        cancelTeleport(player, "damage");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        cancelTeleport(player, "quit");

        cooldowns.remove(uuid);
        blockedPlayers.remove(uuid);

        cleanupPlayerRequests(uuid);

        ignoredPlayers.remove(uuid);
    }

    private void cleanupPlayerRequests(UUID uuid) {
        List<TPARequest> outgoing = outgoingRequests.remove(uuid);
        if (outgoing != null) {
            outgoing.forEach(req -> {
                incomingRequests.computeIfPresent(req.getTarget(), (k, v) -> {
                    v.remove(req);
                    return v.isEmpty() ? null : v;
                });
            });
        }

        List<TPARequest> incoming = incomingRequests.remove(uuid);
        if (incoming != null) {
            incoming.forEach(req -> {
                outgoingRequests.computeIfPresent(req.getRequester(), (k, v) -> {
                    v.remove(req);
                    return v.isEmpty() ? null : v;
                });
            });
        }
    }

    private void startCleanupTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            long now = System.currentTimeMillis();

            List<TPARequest> toRemove = new ArrayList<>();

            incomingRequests.values().forEach(list -> {
                list.stream()
                        .filter(r -> r.isExpired(timeoutDuration) && !r.isExpired())
                        .forEach(toRemove::add);
            });

            toRemove.forEach(req -> {
                notifyExpired(req);
                removeRequestFromBothMaps(req);
            });

            incomingRequests.entrySet().removeIf(e -> e.getValue().isEmpty());
            outgoingRequests.entrySet().removeIf(e -> e.getValue().isEmpty());

            cooldowns.entrySet().removeIf(e -> e.getValue() <= now);
        }, 200L, 200L);
    }

    private void notifyExpired(TPARequest request) {
        Player target = Bukkit.getPlayer(request.getTarget());
        Player requester = Bukkit.getPlayer(request.getRequester());

        if (target != null && requester != null) {
            target.sendMessage(plugin.getLanguageManager().get(target, "tpa.expired.target",
                    Map.of("player", requester.getName())));
            requester.sendMessage(plugin.getLanguageManager().get(requester, "tpa.expired.requester",
                    Map.of("player", target.getName())));
        }
    }

    private void cleanupExpiredForPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        List<TPARequest> outgoing = outgoingRequests.get(uuid);
        if (outgoing != null) {
            outgoing.removeIf(r -> {
                if (r.isExpired(timeoutDuration)) {
                    removeRequestFromBothMaps(r);
                    return true;
                }
                return false;
            });
            if (outgoing.isEmpty()) outgoingRequests.remove(uuid);
        }
    }

    private boolean hasCooldown(Player player) {
        if (player.hasPermission("essentialsc.tpa.bypass.cooldown")) return false;
        Long expiry = cooldowns.get(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    private void setCooldown(Player player) {
        if (cooldownDuration > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldownDuration);
        }
    }

    public List<TPARequest> getIncomingRequests(Player player) {
        List<TPARequest> list = incomingRequests.getOrDefault(player.getUniqueId(), new ArrayList<>());
        list.removeIf(r -> {
            if (r.isExpired(timeoutDuration)) {
                removeRequestFromBothMaps(r);
                return true;
            }
            return false;
        });
        return list;
    }

    public List<TPARequest> getOutgoingRequests(Player player) {
        List<TPARequest> list = outgoingRequests.getOrDefault(player.getUniqueId(), new ArrayList<>());
        list.removeIf(r -> {
            if (r.isExpired(timeoutDuration)) {
                removeRequestFromBothMaps(r);
                return true;
            }
            return false;
        });
        return list;
    }

    public boolean hasIncomingRequests(Player player) {
        return !getIncomingRequests(player).isEmpty();
    }

    public boolean hasOutgoingRequests(Player player) {
        return !getOutgoingRequests(player).isEmpty();
    }

    public Set<UUID> getBlockedPlayers() {
        return new HashSet<>(blockedPlayers);
    }
}