package org.alifmis.calitinaPlugin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import net.kyori.adventure.key.Key;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Beacon;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class ReinforcedPickaxeListener implements Listener {
    private static final long BREAK_DELAY_TICKS = 20L;
    private static final long BEACON_CHECK_DELAY_TICKS = 1L;
    private static final long BEACON_CHECK_PERIOD_TICKS = 5L;

    private final Plugin plugin;
    private final ReinforcedPickaxeItem pickaxeItem;
    private final ReinforcedDeepslateFragmentItem fragmentItem;
    private final ReinforcedIngotItem ingotItem;
    private final Map<BlockKey, UUID> pendingBreaks = new ConcurrentHashMap<>();

    public ReinforcedPickaxeListener(Plugin plugin, ReinforcedPickaxeItem pickaxeItem, ReinforcedDeepslateFragmentItem fragmentItem, ReinforcedIngotItem ingotItem) {
        this.plugin = plugin;
        this.pickaxeItem = pickaxeItem;
        this.fragmentItem = fragmentItem;
        this.ingotItem = ingotItem;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.REINFORCED_DEEPSLATE) {
            return;
        }
        Player player = event.getPlayer();
        if (!pickaxeItem.isReinforcedPickaxe(player.getInventory().getItemInMainHand())) {
            return;
        }
        event.setCancelled(true);

        BlockKey key = BlockKey.from(block);
        if (pendingBreaks.putIfAbsent(key, player.getUniqueId()) != null) {
            return;
        }

        scheduleBreak(player, block.getLocation(), key);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.REINFORCED_DEEPSLATE) {
            return;
        }
        Player player = event.getPlayer();
        BlockKey key = BlockKey.from(block);
        if (player.getUniqueId().equals(pendingBreaks.get(key))) {
            pendingBreaks.remove(key);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.REINFORCED_DEEPSLATE) {
            return;
        }
        if (pickaxeItem.isReinforcedPickaxe(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        if (pickaxeItem.isReinforcedPickaxe(event.getItem())) {
            event.setCancelled(true);
            event.getEnchantsToAdd().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();
        if (pickaxeItem.isReinforcedPickaxe(first) || pickaxeItem.isReinforcedPickaxe(second)) {
            event.setResult(null);
            return;
        }
        ItemStack armor = null;
        if (isArmorItem(first) && ingotItem.isReinforcedIngot(second)) {
            armor = first;
        } else if (isArmorItem(second) && ingotItem.isReinforcedIngot(first)) {
            armor = second;
        }
        if (armor == null) {
            return;
        }
        ItemStack result = createInvisibleArmor(armor);
        if (result == null) {
            return;
        }
        event.setResult(result);
        event.getInventory().setRepairCost(1);
        event.getInventory().setMaximumRepairCost(1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (pickaxeItem.isReinforcedPickaxe(event.getItem())) {
            event.setCancelled(true);
            consumePickaxe(event.getPlayer(), event.getItem());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (!fragmentItem.isReinforcedDeepslateFragment(item.getItemStack())) {
            return;
        }
        item.getScheduler().runAtFixedRate(plugin, task -> {
            if (!item.isValid() || !fragmentItem.isReinforcedDeepslateFragment(item.getItemStack())) {
                task.cancel();
                return;
            }
            if (!isActiveBeaconBelow(item)) {
                return;
            }
            ItemStack stack = item.getItemStack();
            int count = stack.getAmount();
            int ingots = 0;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                if (random.nextBoolean()) {
                    ingots++;
                }
            }
            if (ingots <= 0) {
                item.remove();
            } else {
                item.setItemStack(ingotItem.createItem(ingots));
            }
            task.cancel();
        }, () -> {
        }, BEACON_CHECK_DELAY_TICKS, BEACON_CHECK_PERIOD_TICKS);
    }

    private void scheduleBreak(Player player, Location location, BlockKey key) {
        World world = location.getWorld();
        if (world == null) {
            pendingBreaks.remove(key);
            return;
        }
        plugin.getServer().getRegionScheduler().runDelayed(plugin, location, task -> {
            UUID owner = pendingBreaks.get(key);
            if (owner == null || !owner.equals(player.getUniqueId())) {
                return;
            }
            pendingBreaks.remove(key);
            Block target = world.getBlockAt(location);
            if (target.getType() != Material.REINFORCED_DEEPSLATE) {
                return;
            }
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (!pickaxeItem.isReinforcedPickaxe(mainHand)) {
                return;
            }
            target.setType(Material.AIR, false);
            world.dropItemNaturally(location, fragmentItem.createItem());
            consumePickaxe(player, mainHand);
        }, BREAK_DELAY_TICKS);
    }

    private void consumePickaxe(Player player, ItemStack mainHand) {
        if (!pickaxeItem.isReinforcedPickaxe(mainHand)) {
            return;
        }
        int amount = mainHand.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            mainHand.setAmount(amount - 1);
        }
    }

    private ItemStack createInvisibleArmor(ItemStack armor) {
        ItemStack result = armor.clone();
        EquipmentSlot slot = getArmorSlot(result.getType());
        if (slot == null) {
            return null;
        }
        Equippable.Builder builder = Equippable.equippable(slot)
                .assetId(Key.key("minecraft", "empty"));
        result.setData(DataComponentTypes.EQUIPPABLE, builder);
        result.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, Boolean.FALSE);
        return result;
    }

    private boolean isArmorItem(ItemStack itemStack) {
        return itemStack != null && getArmorSlot(itemStack.getType()) != null;
    }

    private EquipmentSlot getArmorSlot(Material material) {
        if (Tag.ITEMS_HEAD_ARMOR.isTagged(material)) {
            return EquipmentSlot.HEAD;
        }
        if (Tag.ITEMS_CHEST_ARMOR.isTagged(material)) {
            return EquipmentSlot.CHEST;
        }
        if (Tag.ITEMS_LEG_ARMOR.isTagged(material)) {
            return EquipmentSlot.LEGS;
        }
        if (Tag.ITEMS_FOOT_ARMOR.isTagged(material)) {
            return EquipmentSlot.FEET;
        }
        return null;
    }

    private boolean isActiveBeaconBelow(Item item) {
        Block block = item.getLocation().getBlock();
        Block beaconBlock = block.getType() == Material.BEACON ? block : block.getRelative(BlockFace.DOWN);
        if (beaconBlock.getType() != Material.BEACON) {
            return false;
        }
        if (!(beaconBlock.getState() instanceof Beacon beacon)) {
            return false;
        }
        return beacon.getTier() > 0;
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        static BlockKey from(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }
}
