package net.godlycow.org.essc.modules.auction;

import org.bukkit.inventory.ItemStack;
import java.math.BigDecimal;
import java.util.UUID;

public class BuyHistoryEntry {
    private final int auctionId;
    private final UUID buyerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final BigDecimal price;
    private final long timestamp;

    public BuyHistoryEntry(int auctionId, UUID buyerUuid, String sellerName,
                           ItemStack item, BigDecimal price, long timestamp) {
        this.auctionId = auctionId;
        this.buyerUuid = buyerUuid;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.price = price;
        this.timestamp = timestamp;
    }

    public int getAuctionId() {
        return auctionId;
    }
    public UUID getBuyerUuid() {
        return buyerUuid;
    }
    public String getSellerName() {
        return sellerName;
    }
    public ItemStack getItem() {
        return item.clone();
    }
    public BigDecimal getPrice() {
        return price;
    }
    public long getTimestamp() {
        return timestamp;
    }
}