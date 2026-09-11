package net.godlycow.org.essc.modules.auction;

import org.bukkit.inventory.ItemStack;
import java.math.BigDecimal;
import java.util.UUID;

public class AuctionHistory {

    public enum Type {
        SELL, BUY
    }

    private final int id;
    private final UUID playerUuid;
    private final Type type;
    private final ItemStack item;
    private final BigDecimal price;
    private final String otherParty;
    private final long timestamp;
    private final int auctionId;

    public AuctionHistory(int id, UUID playerUuid, Type type, ItemStack item,
                          BigDecimal price, String otherParty, long timestamp, int auctionId) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.type = type;
        this.item = item.clone();
        this.price = price;
        this.otherParty = otherParty;
        this.timestamp = timestamp;
        this.auctionId = auctionId;
    }

    public int getId() {
        return id;
    }
    public Type getType() {
        return type;
    }
    public ItemStack getItem() {
        return item.clone();
    }
    public BigDecimal getPrice() {
        return price;
    }
}