package org.alifmis.calitinaPlugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ReinforcedIngotItem {
    private static final byte KEY_MARKER = 1;

    private final NamespacedKey key;
    private final ItemStack template;

    public ReinforcedIngotItem(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "reinforced_ingot");
        this.template = buildTemplate();
    }

    public ItemStack createItem() {
        return template.clone();
    }

    public ItemStack createItem(int amount) {
        ItemStack item = template.clone();
        item.setAmount(amount);
        return item;
    }

    public boolean isReinforcedIngot(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.IRON_INGOT) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Byte marker = container.get(key, PersistentDataType.BYTE);
        return marker != null && marker == KEY_MARKER;
    }

    private ItemStack buildTemplate() {
        ItemStack item = new ItemStack(Material.IRON_INGOT, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Reinforced Ingot", NamedTextColor.WHITE));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, KEY_MARKER);
        item.setItemMeta(meta);
        return item;
    }
}

