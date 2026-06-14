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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ReinforcedPickaxeListener implements Listener {
    private static final long BREAK_DELAY_TICKS = 20L;
    private static final long BEACON_CHECK_DELAY_TICKS = 1L;
    private static final long BEACON_CHECK_PERIOD_TICKS = 5L;
    private static final Material OMINOUS_VAULT_BLOCK = resolveMaterial("OMINOUS_VAULT", "VAULT");

    private final CalitinaPlugin plugin;
    private final ReinforcedPickaxeItem pickaxeItem;
    private final ReinforcedDeepslateFragmentItem fragmentItem;
    private final ReinforcedIngotItem ingotItem;
    private final Map<BlockKey, UUID> pendingBreaks = new ConcurrentHashMap<>();
    private final Map<BlockKey, Boolean> scheduledVaultChecks = new ConcurrentHashMap<>();

    public ReinforcedPickaxeListener(CalitinaPlugin plugin, ReinforcedPickaxeItem pickaxeItem, ReinforcedDeepslateFragmentItem fragmentItem, ReinforcedIngotItem ingotItem) {
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
        result.setAmount(1);
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

    @EventHandler(ignoreCancelled = true)
    public void onAnvilResultClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory() instanceof AnvilInventory inventory)) {
            return;
        }
        if (event.getSlotType() != InventoryType.SlotType.RESULT) {
            return;
        }
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        ItemStack first = inventory.getFirstItem();
        ItemStack second = inventory.getSecondItem();
        if (first == null || second == null) {
            return;
        }
        ItemStack ingotStack = ingotItem.isReinforcedIngot(first) ? first
                : ingotItem.isReinforcedIngot(second) ? second : null;
        ItemStack armorStack = isArmorItem(first) ? first
                : isArmorItem(second) ? second : null;
        if (ingotStack == null || armorStack == null || !isInvisibleArmorResult(result)) {
            return;
        }
        int remainder = ingotStack.getAmount() - 1;
        if (remainder <= 0) {
            return;
        }
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, task -> {
            ItemStack currentResult = inventory.getItem(2);
            if (currentResult != null && currentResult.getType() != Material.AIR) {
                return;
            }
            ItemStack currentIngot = ingotItem.isReinforcedIngot(inventory.getFirstItem())
                    ? inventory.getFirstItem()
                    : ingotItem.isReinforcedIngot(inventory.getSecondItem())
                    ? inventory.getSecondItem() : null;
            if (currentIngot != null) {
                return;
            }
            for (ItemStack leftover : player.getInventory().addItem(ingotItem.createItem(remainder)).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOminousVaultKeyUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null || !isOminousVault(clicked)) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.OMINOUS_TRIAL_KEY) {
            return;
        }
        BlockKey vaultKey = BlockKey.from(clicked);
        if (scheduledVaultChecks.putIfAbsent(vaultKey, Boolean.TRUE) != null) {
            return;
        }
        Location loc = clicked.getLocation().clone();
        UUID playerId = event.getPlayer().getUniqueId();
        plugin.getServer().getRegionScheduler().runDelayed(plugin, loc, task -> {
            plugin.getServer().getRegionScheduler().runDelayed(plugin, loc,
                    t -> scheduledVaultChecks.remove(vaultKey), 16L);

            World world = loc.getWorld();
            if (world == null) return;
            Block block = world.getBlockAt(loc);
            if (!isOminousVault(block)) return;
            if (!(block.getBlockData() instanceof org.bukkit.block.data.type.Vault vaultData)) return;
            if (vaultData.getVaultState() != org.bukkit.block.data.type.Vault.State.UNLOCKING) return;
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null) return;
            double chance = plugin.getOminousVaultChance();
            if (chance <= 0.0) return;
            if (ThreadLocalRandom.current().nextDouble() >= chance) return;
            ItemStack reward = pickaxeItem.createItem();
            for (ItemStack leftover : player.getInventory().addItem(reward).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            plugin.getLogger().info("Granted Reinforced Pickaxe from ominous vault to " + player.getName());
        }, 1L);
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

    private boolean isInvisibleArmorResult(ItemStack itemStack) {
        Equippable equippable = itemStack.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable == null || !Key.key("minecraft", "empty").equals(equippable.assetId())) {
            return false;
        }
        Boolean glintOverride = itemStack.getData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        return Boolean.FALSE.equals(glintOverride);
    }

    private static Material resolveMaterial(String primary, String fallback) {
        Material material = Material.matchMaterial(primary);
        return material != null ? material : Material.matchMaterial(fallback);
    }

    private boolean isOminousVault(Block block) {
        return OMINOUS_VAULT_BLOCK != null && block.getType() == OMINOUS_VAULT_BLOCK;
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        static BlockKey from(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }
}
