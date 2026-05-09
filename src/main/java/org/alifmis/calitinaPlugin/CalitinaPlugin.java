package org.alifmis.calitinaPlugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class CalitinaPlugin extends JavaPlugin {

    private ReinforcedPickaxeItem pickaxeItem;
    private ReinforcedDeepslateFragmentItem fragmentItem;
    private ReinforcedIngotItem ingotItem;
    private double ominousVaultChance;

    @Override
    public void onEnable() {
        loadSettings();
        pickaxeItem = new ReinforcedPickaxeItem(this);
        fragmentItem = new ReinforcedDeepslateFragmentItem(this);
        ingotItem = new ReinforcedIngotItem(this);
        getServer().getPluginManager().registerEvents(
                new ReinforcedPickaxeListener(this, pickaxeItem, fragmentItem, ingotItem),
                this
        );
        getCommand("reinforcedpickaxe").setExecutor(new ReinforcedPickaxeCommand(pickaxeItem));
        getLogger().info("CalitinaPlugin enabled. Reinforced pickaxe ready.");
    }

    public double getOminousVaultChance() {
        return ominousVaultChance;
    }

    private void loadSettings() {
        saveDefaultConfig();
        reloadConfig();
        ominousVaultChance = getConfig().getDouble("reinforced-pickaxe.ominous-vault-chance", 0.10);
        if (Double.isNaN(ominousVaultChance)) {
            ominousVaultChance = 0.10;
        }
        ominousVaultChance = Math.max(0.0, Math.min(1.0, ominousVaultChance));
        getLogger().info("Ominous vault reinforced pickaxe chance: " + ominousVaultChance);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("CalitinaPlugin disabled.");
    }
}
