package org.alifmis.calitinaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class ReinforcedPickaxeListener implements Listener {
    private static final long BREAK_DELAY_TICKS = 20L;

    private final Plugin plugin;
    private final ReinforcedPickaxeItem pickaxeItem;
    private final Map<BlockKey, UUID> pendingBreaks = new ConcurrentHashMap<>();

    public ReinforcedPickaxeListener(Plugin plugin, ReinforcedPickaxeItem pickaxeItem) {
        this.plugin = plugin;
        this.pickaxeItem = pickaxeItem;
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
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (pickaxeItem.isReinforcedPickaxe(event.getItem())) {
            event.setCancelled(true);
            consumePickaxe(event.getPlayer(), event.getItem());
        }
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
            world.dropItemNaturally(location, new ItemStack(Material.REINFORCED_DEEPSLATE));
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

    private record BlockKey(UUID worldId, int x, int y, int z) {
        static BlockKey from(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }
}
