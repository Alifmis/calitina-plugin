package org.alifmis.calitinaPlugin;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ReinforcedPickaxeItem {
    private static final byte KEY_MARKER = 2;

    private final NamespacedKey key;
    private final ItemStack template;

    public ReinforcedPickaxeItem(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "reinforced_pickaxe");
        this.template = buildTemplate();
    }

    public ItemStack createItem() {
        return template.clone();
    }

    public boolean isReinforcedPickaxe(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.NETHERITE_PICKAXE) {
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
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("特殊的强化镐", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
                Component.text("似乎可以破坏强化深板岩...", NamedTextColor.GRAY),
                Component.text("看起来有点易碎？", NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, KEY_MARKER);
        item.setItemMeta(meta);
        return item;
    }
}
