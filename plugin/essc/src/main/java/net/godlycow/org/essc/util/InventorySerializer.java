package net.godlycow.org.essc.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class InventorySerializer {

    private InventorySerializer() {
        
    }

    public static String serialize(PlayerInventory inventory) {
        ItemStack[] slots = new ItemStack[41];

        ItemStack[] storage = inventory.getStorageContents();
        System.arraycopy(storage, 0, slots, 0, Math.min(storage.length, 36));

        slots[36]= inventory.getHelmet();
        slots[37] = inventory.getChestplate();
        slots[38] = inventory.getLeggings();
        slots[39] = inventory.getBoots();
        slots[40] = inventory.getItemInOffHand();

        return serializeSlots(slots);
    }

    public static String serializeSlots(ItemStack[] slots) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream stream = new BukkitObjectOutputStream(out);

            stream.writeInt(slots.length);

            for (ItemStack slot : slots) {
                stream.writeObject(slot);
            }

            stream.close();

            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize inventory slots", e);
        }
    }

    public static ItemStack[] deserialize(String base64) {

        if (base64 == null || base64.isBlank()) {
            return new ItemStack[41];
        }

        try {
            ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream stream = new BukkitObjectInputStream(in);

            int length = stream.readInt();
            ItemStack[] slots = new ItemStack[length];

            for (int i = 0; i < length; i++) {
                slots[i] = (ItemStack) stream.readObject();
            }

            stream.close();


            return slots;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize inventory", e);
        }
    }

    public static void applyToInventory(ItemStack[] slots, PlayerInventory inventory) {
        ItemStack[] storage = new ItemStack[36];

        for (int i = 0; i < 36 && i < slots.length; i++) {
            storage[i] = slots[i];
        }

        inventory.setStorageContents(storage);

        if (slots.length > 36) inventory.setHelmet(slots[36]);
        if (slots.length > 37) inventory.setChestplate(slots[37]);
        if (slots.length > 38) inventory.setLeggings(slots[38]);

        if (slots.length > 39) inventory.setBoots(slots[39]);

        if (slots.length > 40) {
            inventory.setItemInOffHand(
                    slots[40] != null ? slots[40] : new ItemStack(Material.AIR)
            );
        }
    }

    public static int storageSize() {
        return 36;
    }


    public static int indexHelmet() {
        return 36;
    }

    public static int indexChestplate() {
        return 37;
    }

    public static int indexLeggings() {
        return 38;
    }

    public static int indexBoots() {
        return 39;
    }

    public static int indexOffhand() {
        return 40;
    }
}