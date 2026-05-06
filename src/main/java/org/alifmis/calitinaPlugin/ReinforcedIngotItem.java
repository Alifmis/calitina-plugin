package org.alifmis.calitinaPlugin;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
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
        if (itemStack == null || itemStack.getType() != Material.DIAMOND) {
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
        ItemStack item = new ItemStack(Material.DIAMOND, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("淬炼成功的隐形之石", NamedTextColor.BLUE));
        meta.lore(List.of(
                Component.text("似乎可以用铁砧镶到盔甲之中...", NamedTextColor.GRAY),
                Component.text("可以隐去盔甲的轮廓？", NamedTextColor.GRAY)
                ));
        Enchantment glintEnchantment = Enchantment.getByKey(NamespacedKey.minecraft("mending"));
        if (glintEnchantment != null) {
            meta.addEnchant(glintEnchantment, 1, true);
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, KEY_MARKER);
        item.setItemMeta(meta);
        return item;
    }
}
