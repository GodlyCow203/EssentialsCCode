package net.godlycow.org.essc.modules.auction;

import org.bukkit.inventory.ItemStack;
import java.math.BigDecimal;
import java.util.UUID;

public class SellHistoryEntry {
    private final int auctionId;
    private final UUID sellerUuid;
    private final String buyerName;
    private final ItemStack item;
    private final BigDecimal price;
    private final long timestamp;

    public SellHistoryEntry(int auctionId, UUID sellerUuid, String buyerName,
                            ItemStack item, BigDecimal price, long timestamp) {
        this.auctionId = auctionId;
        this.sellerUuid = sellerUuid;
        this.buyerName = buyerName;
        this.item = item.clone();
        this.price = price;
        this.timestamp = timestamp;
    }

    public int getAuctionId() {
        return auctionId;
    }
    public UUID getSellerUuid() {
        return sellerUuid;
    }
    public String getBuyerName() {
        return buyerName;
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