package net.godlycow.org.essc.api.impl.kit;

import net.godlycow.org.essc.api.kit.Kit;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KitImpl implements Kit {
    private final net.godlycow.org.essc.modules.kit.Kit internalKit;

    public KitImpl(net.godlycow.org.essc.modules.kit.Kit internalKit) {
        this.internalKit = internalKit;
    }

    @Override
    public String getName() {
        String name = internalKit.getName();
        return name;
    }

    @Override
    public String getDisplayName() {
        String displayName = internalKit.getDisplayName();
        return displayName;
    }

    @Override
    public String getRequiredPermission() {
        String permission = internalKit.getPermission();
        return permission;
    }

    @Override
    public long getCooldownInSeconds() {
        long cooldown = internalKit.getCooldown();
        return cooldown;
    }

    @Override
    public boolean isOneTimeUse() {
        boolean oneTime = internalKit.isOneTime();
        return oneTime;
    }

    @Override
    public boolean isGrantedOnFirstJoin() {
        boolean firstJoin = internalKit.isFirstJoin();
        return firstJoin;
    }

    @Override
    public int getMaximumClaimsAllowed() {
        int maxClaims = internalKit.getMaxClaims();
        return maxClaims;
    }

    @Override
    public List<ItemStack> getItemStacks() {
        List<ItemStack> items = internalKit.getItems();
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    @Override
    public String getKitDescription() {
        String description = internalKit.getDescription();
        return description;
    }

    @Override
    public boolean isSynchronizedAcrossNetwork() {
        boolean networkSync = internalKit.isNetworkSync();
        return networkSync;
    }

    @Override
    public int getGuiSlot() {
        return internalKit.getGuiSlot();
    }

    @Override
    public int getGuiPage() {
        return internalKit.getGuiPage();
    }

    public net.godlycow.org.essc.modules.kit.Kit getInternalKit() {
        return internalKit;
    }
}