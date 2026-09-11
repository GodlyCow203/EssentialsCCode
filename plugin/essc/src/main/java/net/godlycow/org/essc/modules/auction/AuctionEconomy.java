package net.godlycow.org.essc.modules.auction;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionEconomy {
    private final EssentialsC plugin;
    private final Map<Integer, Object> processingLocks = new ConcurrentHashMap<>();

    public AuctionEconomy(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public boolean tryLockAuction(int auctionId) {
        return processingLocks.putIfAbsent(auctionId, Boolean.TRUE) == null;
    }

    public void unlockAuction(int auctionId) {
        processingLocks.remove(auctionId);
    }

    public CompletableFuture<Boolean> processPurchase(Player buyer, Auction auction) {
        if (!tryLockAuction(auction.getId())) {
            return CompletableFuture.completedFuture(false);
        }

        if (plugin.getEconomyManager() == null) {
            unlockAuction(auction.getId());
            plugin.getLogger().severe("Economy manager not available, cannot process auction purchase");
            return CompletableFuture.completedFuture(false);
        }

        return plugin.getEconomyManager().has(buyer.getUniqueId(), auction.getPrice())
                .thenCompose(has -> {
                    if (!has) {
                        unlockAuction(auction.getId());
                        return CompletableFuture.completedFuture(false);
                    }
                    return executeTransfer(buyer, auction);
                })
                .exceptionally(ex -> {
                    unlockAuction(auction.getId());
                    plugin.getLogger().severe("Buy error for auction " + auction.getId() + ": " + ex.getMessage());
                    return false;
                });
    }

    private CompletableFuture<Boolean> executeTransfer(Player buyer, Auction auction) {
        return plugin.getEconomyManager().withdraw(buyer.getUniqueId(), auction.getPrice())
                .thenCompose(withdrawn -> {
                    if (!withdrawn) {
                        unlockAuction(auction.getId());
                        return CompletableFuture.completedFuture(false);
                    }
                    return plugin.getEconomyManager().deposit(auction.getSellerUuid(), auction.getPrice())
                            .thenCompose(deposited -> finalizeTransfer(buyer, auction, deposited));
                });
    }

    private CompletableFuture<Boolean> finalizeTransfer(Player buyer, Auction auction, boolean deposited) {
        if (!deposited) {
            plugin.getEconomyManager().deposit(buyer.getUniqueId(), auction.getPrice());
            unlockAuction(auction.getId());
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.completedFuture(true);
    }

    public void deliverItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(drop ->
                player.getWorld().dropItemNaturally(player.getLocation(), drop));
    }
}